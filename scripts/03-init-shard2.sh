#!/bin/bash
set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
"${SCRIPT_DIR}/wait-for-mongo.sh" shard2 27020

echo "=== Initiating shard2 replica set (shard2ReplSet) ==="
docker exec shard2 mongosh --quiet --port 27020 --eval '
rs.initiate({
  _id: "shard2ReplSet",
  members: [
    { _id: 0, host: "shard2:27020" }
  ]
});
'
echo "shard2ReplSet initiated."
