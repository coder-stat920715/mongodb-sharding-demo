#!/bin/bash
# Prints cluster / shard-distribution diagnostics useful for the interview demo.
set -e
echo "=== sh.status() ==="
docker exec mongos mongosh --quiet --port 27017 --eval 'sh.status()'

echo ""
echo "=== Shard distribution for ecommerce_db.orders ==="
docker exec mongos mongosh --quiet --port 27017 --eval '
db.getSiblingDB("ecommerce_db").orders.getShardDistribution();
'

echo ""
echo "=== Chunk counts per shard ==="
docker exec mongos mongosh --quiet --port 27017 --eval '
db.getSiblingDB("config").chunks.aggregate([
  { $match: { ns: "ecommerce_db.orders" } },
  { $group: { _id: "$shard", chunkCount: { $sum: 1 } } }
]).forEach(printjson);
'
