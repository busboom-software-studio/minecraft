#!/usr/bin/env python3
"""Camera bridge — lets Claude take photos with the USB camera.

macOS only hands camera access to an application with its own identity, and a bare
ffmpeg spawned by another app never registers a request at all (it just receives no
frames, forever). Running this from Terminal.app makes Terminal the requesting app,
which is something macOS will actually prompt about.

Start it once:

    python3 scripts/camera-bridge.py

Grant the camera prompt when it appears. After that it stays running and watches for
a trigger file, exactly like the F2 screenshot bridge in run/:

    touch run/claude-camera     ->  a photo appears in run/photos/

Every camera listed in CAMERA_DEVICE shoots on each trigger, so a rig with one
camera on the side and one overhead gets both angles of the same moment. Files
are tagged with the camera they came from.

    echo 30:4 > run/claude-camera-burst   ->  30s at 4fps into run/photos/burst_*/

Bursts record from every camera at once, each into its own subdirectory.

Stop it with Ctrl-C.
"""

import os
import re
import shutil
import subprocess
import sys
import tempfile
import time
from datetime import datetime

HERE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TRIGGER = os.path.join(HERE, "run", "claude-camera")
BURST_TRIGGER = os.path.join(HERE, "run", "claude-camera-burst")
PHOTO_DIR = os.path.join(HERE, "run", "photos")
# Indices are assigned in whatever order AVFoundation enumerates and DO shuffle between
# runs, so match on name instead. CAMERA_DEVICE is a comma-separated list of name
# fragments (or indices); every one of them shoots on each trigger.
DEVICE_WANTED = os.environ.get("CAMERA_DEVICE", "Global Shutter Camera,HD Pro Webcam")

# The camera rejects some combinations outright, so try the most specific first and
# fall back. Whichever one produces a frame gets reused for the rest of the session.
ATTEMPTS = [
    ["-pixel_format", "uyvy422", "-framerate", "30"],
    ["-pixel_format", "uyvy422", "-video_size", "1280x720", "-framerate", "30"],
    ["-pixel_format", "uyvy422", "-video_size", "640x480", "-framerate", "30"],
    ["-pixel_format", "nv12", "-framerate", "30"],
    [],
]

# The first frames off a webcam are usually black while exposure settles.
WARMUP_FRAMES = 12

CAMS = []   # [Cam], resolved from DEVICE_WANTED at startup


class Cam:
    """One camera, and the ffmpeg input mode that turned out to work for it.

    The working mode is per-camera: a C920 and a capture stick rarely accept the
    same pixel format, so each one keeps its own answer once it has been found.
    """

    def __init__(self, index, name):
        self.index = index
        self.name = name
        # Hand ffmpeg the NAME, never the index. AVFoundation renumbers devices
        # while the bridge is running — observed swapping between two captures
        # seconds apart, so that alternate frames of a run came off the wrong
        # camera and were filed under the right one's label. Re-resolving the
        # index before each shot was not enough, because it can change again
        # between one camera's capture and the next one's a few seconds later.
        # Names are stable, and avfoundation accepts them directly.
        self.source = name
        self.label = re.sub(r"[^a-z0-9]+", "_", name.lower()).strip("_")
        self.args = None

    def __str__(self):
        return f"[{self.index}] {self.name}"


def list_devices():
    out = subprocess.run(
        ["ffmpeg", "-hide_banner", "-f", "avfoundation", "-list_devices", "true", "-i", ""],
        capture_output=True, text=True,
    )
    return out.stderr


def video_devices():
    """[(index, name)] for video only — audio has its own separate numbering, and the
    same camera can appear in both lists under different indices."""
    found, in_video = [], False
    for line in list_devices().splitlines():
        if "AVFoundation video devices" in line:
            in_video = True
            continue
        if "AVFoundation audio devices" in line:
            break
        if in_video and "] [" in line:
            tail = line.split("] [", 1)[1]
            idx, name = tail.split("] ", 1)
            found.append((idx.strip(), name.strip()))
    return found


def present(cams):
    """Drop any camera that has been unplugged since startup."""
    names = {name for _, name in video_devices()}
    alive = []
    for cam in cams:
        if cam.name in names:
            alive.append(cam)
        else:
            print(f"  WARNING: {cam.name} is gone, skipping it", flush=True)
    return alive


def resolve_devices(wanted):
    """Turn a comma-separated list of name fragments into [Cam]. Indices pass through.

    A fragment that matches nothing is fatal — silently shooting one angle when the
    rig has two is worse than refusing to start, because you only find out later
    when half the frames are missing.
    """
    devices = video_devices()
    names = ", ".join(f"[{i}] {n}" for i, n in devices) or "none found"
    cams = []
    for frag in [w.strip() for w in wanted.split(",") if w.strip()]:
        if frag.isdigit():
            cams.append(Cam(frag, dict(devices).get(frag, "?")))
            continue
        for idx, name in devices:
            if frag.lower() in name.lower():
                cams.append(Cam(idx, name))
                break
        else:
            sys.exit(f"No video device matching {frag!r}. Available: {names}")
    if not cams:
        sys.exit(f"CAMERA_DEVICE is empty. Available: {names}")
    return cams


def grab(cam, args, dest):
    """Capture into a temp dir and keep the last (settled) frame. True if it worked."""
    with tempfile.TemporaryDirectory() as tmp:
        cmd = (["ffmpeg", "-hide_banner", "-loglevel", "error", "-f", "avfoundation"]
               + args + ["-i", cam.source, "-frames:v", str(WARMUP_FRAMES), "-q:v", "2",
                         os.path.join(tmp, "f_%03d.jpg")])
        try:
            subprocess.run(cmd, capture_output=True, text=True, timeout=25)
        except subprocess.TimeoutExpired:
            return False
        frames = sorted(f for f in os.listdir(tmp) if f.endswith(".jpg"))
        if not frames:
            return False
        shutil.copy(os.path.join(tmp, frames[-1]), dest)
        return True


def burst(seconds, fps, outdir):
    """Record continuously while the subject is turned, sampling `fps` frames a second.

    Far better than single shots for a 3D subject: one slow rotation gives every angle,
    including the in-between ones that single shots miss, and nobody has to hold still.
    """
    os.makedirs(outdir, exist_ok=True)
    # Every camera rolls at once, so the frame at index i from the side camera and
    # the one from the top camera are the same instant of the same rotation.
    procs = []
    for cam in present(CAMS):
        d = os.path.join(outdir, cam.label)
        os.makedirs(d, exist_ok=True)
        args = cam.args or ATTEMPTS[0]
        # Record a couple of extra seconds; the opening frames are still auto-exposing.
        cmd = (["ffmpeg", "-hide_banner", "-loglevel", "error", "-f", "avfoundation"]
               + args + ["-i", cam.source, "-t", str(seconds + 2), "-vf", f"fps={fps}",
                         "-q:v", "2", os.path.join(d, "frame_%04d.jpg")])
        procs.append((cam, d, subprocess.Popen(cmd, stdout=subprocess.DEVNULL,
                                               stderr=subprocess.DEVNULL)))

    counts = {}
    for cam, d, proc in procs:
        try:
            proc.wait(timeout=seconds + 45)
        except subprocess.TimeoutExpired:
            proc.kill()
        frames = sorted(f for f in os.listdir(d) if f.endswith(".jpg"))
        for stale in frames[:2]:           # drop the under-exposed opening frames
            os.remove(os.path.join(d, stale))
        counts[cam.label] = max(0, len(frames) - 2)
    return counts


def capture_one(cam, dest):
    """Shoot `cam` to `dest`, remembering whichever input mode worked for it."""
    for args in ([cam.args] if cam.args else ATTEMPTS):
        if grab(cam, args, dest):
            cam.args = args            # stick with the mode that worked
            return True
    cam.args = None                    # it may come back; re-probe next time
    return False


def capture(stem):
    """Shoot every camera. `stem` is a path without extension; each adds its label.

    Returns the files written. One camera failing doesn't cancel the others — a
    partial set of angles still beats none.
    """
    written = []
    for cam in present(CAMS):
        dest = f"{stem}_{cam.label}.jpg" if len(CAMS) > 1 else f"{stem}.jpg"
        if capture_one(cam, dest):
            written.append(dest)
        else:
            print(f"  {cam} failed", flush=True)
    return written


def main():
    os.makedirs(PHOTO_DIR, exist_ok=True)
    if not shutil.which("ffmpeg"):
        sys.exit("ffmpeg not found — install it with: brew install ffmpeg")

    global CAMS
    print("Video devices:")
    for idx, name in video_devices():
        print(f"  [{idx}] {name}")

    CAMS = resolve_devices(DEVICE_WANTED)
    print("\nUsing:")
    for cam in CAMS:
        print(f"  {cam}  -> {cam.label}")
    print("\nTaking a test photo — allow the camera prompt if it appears.")

    written = capture(os.path.join(PHOTO_DIR, "test"))
    if not written:
        sys.exit("\nNo frames captured. If no prompt appeared, check\n"
                 "System Settings > Privacy & Security > Camera and enable Terminal,\n"
                 "then quit Terminal completely and run this again.")
    if len(written) < len(CAMS):
        print(f"WARNING: only {len(written)} of {len(CAMS)} cameras produced a frame.")

    print("Cameras work. Test photos:")
    for f in written:
        print(f"  {f}")
    print(f"\nWatching for {TRIGGER}\nLeave this running. Ctrl-C to stop.\n")

    shot = 0
    while True:
        if os.path.exists(BURST_TRIGGER):
            try:
                spec = open(BURST_TRIGGER).read().strip()
                os.remove(BURST_TRIGGER)
            except OSError:
                spec = ""
            secs, _, rate = spec.partition(":")
            secs = int(secs) if secs.isdigit() else 20
            rate = rate if rate else "2"
            stamp = datetime.now().strftime("%H%M%S")
            outdir = os.path.join(PHOTO_DIR, f"burst_{stamp}")
            print(f"[{stamp}] RECORDING {secs}s at {rate}fps — turn the robot slowly now",
                  flush=True)
            counts = burst(secs, rate, outdir)
            summary = ", ".join(f"{label} {n}" for label, n in counts.items())
            print(f"  {summary} frames -> {os.path.basename(outdir)}/", flush=True)

        if os.path.exists(TRIGGER):
            try:
                os.remove(TRIGGER)
            except OSError:
                pass
            shot += 1
            stamp = datetime.now().strftime("%H%M%S")
            stem = os.path.join(PHOTO_DIR, f"photo_{shot:03d}_{stamp}")
            print(f"[{stamp}] capturing -> {os.path.basename(stem)}*", flush=True)
            if not capture(stem):
                print("  capture failed on every camera", flush=True)
        time.sleep(0.25)


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\nstopped")
