#!/usr/bin/env bash
# Copy a committed world snapshot from worlds/ back into run/saves/ so the game can load it.
#
#   scripts/restore-world.sh              # restores "New World"
#   scripts/restore-world.sh "Other World"
#
# Refuses to run while the game is open, because the game would overwrite the restored files.
set -euo pipefail
cd "$(dirname "$0")/.."

WORLD="${1:-New World}"
SRC="worlds/$WORLD"
DEST="run/saves/$WORLD"

[ -d "$SRC" ] || { echo "No snapshot for: $SRC" >&2; exit 1; }
if pgrep -f devlaunchinjector >/dev/null 2>&1; then
	echo "Quit the game first; it is still running." >&2
	exit 1
fi

if [ -d "$DEST" ]; then
	BACKUP="run/saves/$WORLD.before-restore-$(date '+%Y%m%d-%H%M%S')"
	echo "Keeping the current save as: $BACKUP"
	mv "$DEST" "$BACKUP"
fi
mkdir -p "$DEST"
rsync -a "$SRC/" "$DEST/"
echo "Restored '$WORLD'. Launch with: ./gradlew runClient -Pworld=\"$WORLD\""
