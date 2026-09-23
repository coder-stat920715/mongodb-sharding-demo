#!/bin/bash
# Master orchestration script: brings up a fully configured MongoDB sharded
# cluster (1 config server, 2 shards, 1 mongos router) ready for the
# ecommerce_db.orders sharding demo used by the Spring Boot application.
set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

echo "############################################################"
echo "# 1/5  Starting containers via docker compose"
echo "############################################################"
(cd "${ROOT_DIR}" && docker compose up -d)

echo "############################################################"
echo "# 2/5  Initializing config server replica set"
echo "############################################################"
"${SCRIPT_DIR}/01-init-configsvr.sh"

echo "############################################################"
echo "# 3/5  Initializing shard1 replica set"
echo "############################################################"
"${SCRIPT_DIR}/02-init-shard1.sh"

echo "############################################################"
echo "# 4/5  Initializing shard2 replica set"
echo "############################################################"
"${SCRIPT_DIR}/03-init-shard2.sh"

echo "############################################################"
echo "# 5/5  Registering shards with mongos + sharding ecommerce_db.orders"
echo "############################################################"
"${SCRIPT_DIR}/04-add-shards-and-shard-collection.sh"

echo ""
echo "============================================================"
echo " Sharded cluster is READY."
echo " mongos entry point: mongodb://localhost:27017/ecommerce_db"
echo " Sharded collection: ecommerce_db.orders"
echo " Shard key: { region: 1, customerId: 1 }"
echo "============================================================"
