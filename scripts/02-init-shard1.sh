#!/bin/bash
set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
"${SCRIPT_DIR}/wait-for-mongo.sh" shard1 27018

echo "=== Initiating shard1 replica set (shard1ReplSet) ==="
docker exec shard1 mongosh --quiet --port 27018 --eval '
rs.initiate({
  _id: "shard1ReplSet",
  members: [
    { _id: 0, host: "shard1:27018" }
  ]
});
'
echo "shard1ReplSet initiated."
