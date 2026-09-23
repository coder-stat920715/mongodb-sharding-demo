#!/bin/bash
set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
"${SCRIPT_DIR}/wait-for-mongo.sh" configsvr 27019

echo "=== Initiating config server replica set (configReplSet) ==="
docker exec configsvr mongosh --quiet --port 27019 --eval '
rs.initiate({
  _id: "configReplSet",
  configsvr: true,
  members: [
    { _id: 0, host: "configsvr:27019" }
  ]
});
'
echo "Config server replica set initiated."
