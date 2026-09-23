#!/bin/bash
set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
"${SCRIPT_DIR}/wait-for-mongo.sh" mongos 27017

echo "=== Registering shard1ReplSet and shard2ReplSet with mongos ==="
docker exec mongos mongosh --quiet --port 27017 --eval '
sh.addShard("shard1ReplSet/shard1:27018");
sh.addShard("shard2ReplSet/shard2:27020");
'

echo "=== Enabling sharding on ecommerce_db ==="
docker exec mongos mongosh --quiet --port 27017 --eval '
sh.enableSharding("ecommerce_db");
'

echo "=== Creating orders collection and building shard key index ==="
docker exec mongos mongosh --quiet --port 27017 --eval '
db.getSiblingDB("ecommerce_db").createCollection("orders");
db.getSiblingDB("ecommerce_db").orders.createIndex({ region: 1, customerId: 1 });
'

echo "=== Sharding ecommerce_db.orders on compound key { region: 1, customerId: 1 } ==="
docker exec mongos mongosh --quiet --port 27017 --eval '
sh.shardCollection("ecommerce_db.orders", { region: 1, customerId: 1 });
'

echo "=== Cluster status ==="
docker exec mongos mongosh --quiet --port 27017 --eval 'sh.status()'
