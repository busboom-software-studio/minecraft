#!/usr/bin/env bash
# Snapshot a dev world from run/saves/ into worlds/ (tracked by git) and commit it.
#
#   scripts/save-world.sh              # saves "New World"
#   scripts/save-world.sh "Other World"
#
# If the game is running with the dev command bridge, the world is flushed to disk first so the
# snapshot is current. Otherwise the snapshot is whatever the game last wrote (it autosaves every
# 5 minutes and on quit).
set -euo pipefail
cd "$(dirname "$0")/.."

WORLD="${1:-New World}"
SRC="run/saves/$WORLD"
DEST="worlds/$WORLD"

[ -d "$SRC" ] || { echo "No such world: $SRC" >&2; exit 1; }

if pgrep -f devlaunchinjector >/dev/null 2>&1; then
	echo "Game is running; asking it to flush the world to disk..."
	SAVES_BEFORE=$(grep -c 'world save complete' run/logs/latest.log 2>/dev/null || true)
	echo '!save' >> run/claude-commands.txt
	for _ in $(seq 1 30); do
		sleep 1
		if grep -q "world save complete" run/logs/latest.log 2>/dev/null \
			&& [ "$(grep -c 'world save complete' run/logs/latest.log)" -gt "${SAVES_BEFORE:-0}" ]; then
			break
		fi
	done
	sleep 1
fi

mkdir -p "$DEST"
# session.lock belongs to the running game; everything else is world data.
rsync -a --delete --exclude session.lock "$SRC/" "$DEST/"

git add "$DEST"
if git diff --cached --quiet; then
	echo "World snapshot unchanged; nothing to commit."
else
	git commit -q -m "Snapshot world: $WORLD ($(date '+%Y-%m-%d %H:%M'))"
	echo "Committed snapshot of '$WORLD' ($(du -sh "$DEST" | cut -f1))."
fi
