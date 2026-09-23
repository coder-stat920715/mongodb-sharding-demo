#!/bin/bash
# Seeds orders across two distinct region values so that, after the balancer
# has settled, documents physically live on both shard1 and shard2 - making
# the scatter-gather demo meaningful (a query with only 1 shard populated
# would not actually prove fan-out behavior).
set -e
BASE_URL=${1:-http://localhost:8080}

post_order() {
  curl -s -o /dev/null -w "%{http_code} " -X POST "${BASE_URL}/api/orders" \
    -H "Content-Type: application/json" \
    -d "$1"
  echo "-> $1"
}

echo "Seeding US-region orders (customers C100-C104)..."
for i in 100 101 102 103 104; do
  post_order "{\"orderId\":\"US-ORD-${i}\",\"region\":\"US\",\"customerId\":\"C${i}\",\"amount\":$((RANDOM % 500 + 20)).99,\"status\":\"PENDING\"}"
done

echo "Seeding EU-region orders (customers C200-C204)..."
for i in 200 201 202 203 204; do
  post_order "{\"orderId\":\"EU-ORD-${i}\",\"region\":\"EU\",\"customerId\":\"C${i}\",\"amount\":$((RANDOM % 500 + 20)).99,\"status\":\"SHIPPED\"}"
done

echo "Seeding APAC-region orders (customers C300-C304)..."
for i in 300 301 302 303 304; do
  post_order "{\"orderId\":\"APAC-ORD-${i}\",\"region\":\"APAC\",\"customerId\":\"C${i}\",\"amount\":$((RANDOM % 500 + 20)).99,\"status\":\"PENDING\"}"
done

echo ""
echo "Seed complete. Try:"
echo "  curl \"${BASE_URL}/api/orders/targeted?region=US&customerId=C100\""
echo "  curl \"${BASE_URL}/api/orders/scatter-gather?status=PENDING\""
