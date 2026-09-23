#!/bin/bash
# Stops and removes all containers, networks and volumes for a clean slate.
set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
(cd "${ROOT_DIR}" && docker compose down -v)
echo "Cluster torn down and volumes removed."
