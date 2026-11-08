#!/bin/sh
set -eu
PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
exec "$PROJECT_DIR/.semantic-runtime/venv/bin/python" "$PROJECT_DIR/scripts/semantic/server.py" \
  --model-dir "$PROJECT_DIR/.semantic-runtime/model" \
  --catalog "$PROJECT_DIR/src/main/resources/kb_starbanking_menu_structure.json" \
  --port "${MENU_SEMANTIC_PORT:-18083}"
