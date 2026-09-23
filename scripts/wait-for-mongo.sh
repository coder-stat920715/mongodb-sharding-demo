#!/bin/bash
# Polls a mongod/mongos container on a given port until it responds to ping.
# Usage: wait-for-mongo.sh <container_name> <port> [timeout_seconds]
CONTAINER=$1
PORT=$2
TIMEOUT=${3:-90}
ELAPSED=0

echo "Waiting for ${CONTAINER}:${PORT} to accept connections..."
until docker exec "${CONTAINER}" mongosh --quiet --port "${PORT}" --eval "db.adminCommand('ping')" > /dev/null 2>&1; do
  sleep 2
  ELAPSED=$((ELAPSED + 2))
  if [ "${ELAPSED}" -ge "${TIMEOUT}" ]; then
    echo "ERROR: ${CONTAINER}:${PORT} did not become ready within ${TIMEOUT}s"
    exit 1
  fi
done
echo "${CONTAINER}:${PORT} is ready."
