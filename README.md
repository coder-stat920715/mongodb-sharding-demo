# MongoDB Sharding Demo — Spring Boot 3.x + Sharded Cluster

A hands-on, runnable project demonstrating NoSQL horizontal scaling concepts for a
Senior Java Developer / Lead Architect interview: cluster topology, shard key
design, query routing (targeted vs. scatter-gather), and explain-plan analysis.

## 1. Architecture

```
                        ┌────────────────────┐
   Spring Boot App  --> │   mongos (27017)   │   <-- single entry point
   (MongoTemplate)      └──────────┬─────────┘
                                    │  reads chunk metadata from
                          ┌─────────┴─────────┐
                          │  configsvr (27019) │  configReplSet
                          └─────────┬─────────┘
                     routes to owning shard(s)
              ┌─────────────────────┴─────────────────────┐
              │                                            │
     ┌────────▼─────────┐                        ┌─────────▼────────┐
     │  shard1 (27018)   │                        │  shard2 (27020)   │
     │  shard1ReplSet     │                        │  shard2ReplSet    │
     └───────────────────┘                        └───────────────────┘

 Sharded collection : ecommerce_db.orders
 Shard key          : { region: 1, customerId: 1 }  (compound, ranged)
```

The Spring Boot application **only ever talks to `mongos`** (`spring.data.mongodb.uri` in
`application.yml`). It has no awareness of which physical shard holds which document —
that routing decision is made entirely by `mongos`, using chunk metadata it reads from the
config server replica set.

## 2. Project Layout

```
mongodb-sharding-demo/
├── docker-compose.yml
├── scripts/
│   ├── wait-for-mongo.sh                       # polling helper
│   ├── 01-init-configsvr.sh                    # rs.initiate() on config server
│   ├── 02-init-shard1.sh                       # rs.initiate() on shard1
│   ├── 03-init-shard2.sh                       # rs.initiate() on shard2
│   ├── 04-add-shards-and-shard-collection.sh   # sh.addShard + sh.shardCollection
│   ├── init-sharded-cluster.sh                 # master script: runs all of the above
│   ├── verify-sharding.sh                      # sh.status() + getShardDistribution()
│   ├── seed-demo-data.sh                       # posts sample orders across 3 regions
│   └── teardown.sh                             # docker compose down -v
└── spring-boot-app/
    ├── pom.xml
    └── src/main/java/com/souptik/shardingdemo/
        ├── ShardingDemoApplication.java
        ├── annotation/Sharded.java              # documents shard key on the entity
        ├── bootstrap/ShardedEntityInspector.java# logs @Sharded entities at startup
        ├── config/MongoConfig.java
        ├── model/Order.java
        ├── repository/OrderRepository.java
        ├── service/OrderService.java            # routing demo logic lives here
        ├── controller/OrderController.java
        ├── controller/GlobalExceptionHandler.java
        └── dto/{ExplainSummary,ApiError}.java
```

## 3. Step-by-Step: Running the Demo

### Prerequisites
- Docker + Docker Compose v2
- Java 17+ and Maven 3.9+ (or use `./mvnw` if you generate a wrapper)
- `curl` (or Postman) for hitting the API

### Step 1 — Start the cluster
```bash
cd mongodb-sharding-demo
chmod +x scripts/*.sh
./scripts/init-sharded-cluster.sh
```
This single script: brings up all 4 containers, initiates all 3 replica sets
(config server, shard1, shard2), registers both shards with `mongos`, enables
sharding on `ecommerce_db`, and shards `orders` on `{ region: 1, customerId: 1 }`.
It finishes by printing `sh.status()` so you can see the cluster topology.

### Step 2 — Verify the cluster (optional but recommended for the interview demo)
```bash
./scripts/verify-sharding.sh
```

### Step 3 — Build and run the Spring Boot app
```bash
cd spring-boot-app
mvn clean package -DskipTests
java -jar target/mongodb-sharding-demo.jar
```
Watch the startup logs — `ShardedEntityInspector` will print the discovered
shard key metadata for the `Order` entity.

### Step 4 — Seed demo data across regions (hence across shards)
```bash
cd ..
./scripts/seed-demo-data.sh
```

### Step 5 — Demonstrate routing behavior

**Targeted write:**
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"orderId":"US-ORD-999","region":"US","customerId":"C999","amount":149.50,"status":"PENDING"}'
```

**Single-shard lookup (shard key present):**
```bash
curl "http://localhost:8080/api/orders/targeted?region=US&customerId=C100"
```

**Scatter-gather anti-pattern (shard key absent):**
```bash
curl "http://localhost:8080/api/orders/scatter-gather?status=PENDING"
```

**Explain plan — targeted query (expect `SINGLE_SHARD`):**
```bash
curl "http://localhost:8080/api/orders/explain?region=US&customerId=C100" | jq .topLevelStage
```

**Explain plan — scatter-gather query (expect `SHARD_MERGE`, multiple shardNames):**
```bash
curl "http://localhost:8080/api/orders/explain/scatter-gather?status=PENDING" | jq '{topLevelStage, shardNames}'
```

**Physical shard distribution:**
```bash
curl "http://localhost:8080/api/orders/shard-distribution" | jq .
```

### Step 6 — Tear down
```bash
./scripts/teardown.sh
```

---

## 4. Advanced Interview Topics

### 4.1 Shard Key Selection Trade-offs

| Strategy | Pros | Cons | When to use |
|---|---|---|---|
| **Ranged** (e.g. `{ region: 1, customerId: 1 }`) | Efficient range queries; related data co-located in contiguous chunks | Risk of hotspotting if the leading field has low cardinality or is monotonically increasing | Query patterns that filter/sort by ranges (e.g. `region` + `date`) |
| **Hashed** (e.g. `{ customerId: "hashed" }`) | Near-uniform write distribution across shards; eliminates hotspots from monotonic keys | Destroys range-query locality — a range scan on the hashed field becomes a scatter-gather | High-throughput, evenly distributed writes where range scans on that field aren't needed |
| **Compound** (this project's choice) | Balances targeting granularity with distribution; supports both equality lookups and prefix range queries | Only the **prefix** of the compound key can be used for query targeting (a query on `customerId` alone, without `region`, still scatters) | Multi-tenant / multi-region systems where most queries naturally know the leading field |

**Avoiding monotonically increasing key hotspots:** if you shard on something
like `_id` (ObjectId, which is time-correlated) or an auto-incrementing order
number, every new insert has a key value greater than any existing one. Under
range-based sharding, that means **every recent write lands in the same
"rightmost" chunk on the same shard** until it splits — turning writes into a
single-shard bottleneck and defeating the purpose of sharding. Fixes:
1. **Hash the key** (`sh.shardCollection(ns, { key: "hashed" })`) so insertion
   order no longer correlates with key order.
2. **Prefix with a naturally distributed field** — which is exactly why this
   project puts `region` first: even though `customerId` might correlate with
   signup order within a region, `region` itself isn't monotonic, so writes
   spread across the shards that own each region's chunks.
3. Use a **compound key with a random/hashed low-cardinality prefix**
   bucketing strategy for extreme write-heavy workloads (e.g. IoT time series).

### 4.2 Jumbo Chunks & Resharding

- A **chunk** is a contiguous range of shard key values (default target size 128 MB).
  The balancer automatically splits chunks that grow too large and migrates
  chunks between shards to keep document/data distribution even.
- A chunk becomes **"jumbo"** when it exceeds the split threshold but MongoDB
  *cannot* split it further — almost always because too many documents share
  the exact same shard key value (a low-cardinality shard key). Jumbo chunks
  can't be migrated by the balancer, which creates a permanent imbalance and,
  in the worst case, a hotspot that can't self-heal.
- **Mitigation:** choose a shard key with sufficiently high cardinality that no
  single key value ever accumulates enough documents to exceed the chunk size
  limit (this is another reason the compound key here uses `customerId`, which
  has far higher cardinality than `region` alone).
- **Resharding:** since MongoDB 5.0, `reshardCollection` allows changing a
  collection's shard key in place (previously this required a full
  dump/restore into a new sharded collection). It works by cloning data under
  the new key into a temporary collection alongside the live one, tailing the
  oplog to stay in sync, then atomically cutting over — with configurable
  downtime characteristics depending on cluster size and write load.
- The balancer runs in the background continuously (subject to a configurable
  balancing window) and migrates chunks via a two-phase, resumable protocol
  that keeps the source shard serving reads/writes until the destination shard
  has fully caught up, minimizing client-visible disruption.

### 4.3 Write Concern & Read Preference in a Sharded Environment

- **Write Concern** (`w`) controls how many replica set members must
  acknowledge a write before mongos reports success back to the client.
  - `w: "majority"` — waits for acknowledgment from a majority of voting
    members of the shard's replica set that owns the affected chunk. This is
    the safe default for anything that must survive a primary failover; it's
    what this project implicitly relies on for the targeted-write demo.
  - For multi-shard writes (e.g. transactions spanning shards), majority write
    concern is essentially mandatory for durability guarantees to hold
    cluster-wide.
- **Read Preference** controls which replica set member(s) within each
  involved shard serve a read.
  - `primary` (default) — always reads the shard's primary; strongly
    consistent, but adds load to primaries.
  - `secondaryPreferred` — prefers a secondary, falling back to the primary if
    none are available; reduces primary load and is useful for
    read-heavy, eventually-consistent-tolerant workloads like the
    scatter-gather status dashboard in this demo.
  - In a **sharded** cluster, read preference is applied *independently on
    each shard contacted* — so a scatter-gather query with
    `secondaryPreferred` may read from a secondary on shard1 and the primary
    on shard2 if shard2 has no healthy secondary at that moment. This
    per-shard independence is a common interview trap: read preference is not
    a single cluster-wide guarantee, it's evaluated shard-by-shard.
- Practical guidance: use `majority` write concern for financial/order data
  (as here), and reserve `secondaryPreferred` reads for analytics/reporting
  paths that can tolerate slightly stale data in exchange for reduced primary
  load — never for the single-shard targeted lookups that back
  customer-facing "view my order" flows.

## 5. Quick Reference — Interview Talking Points

- **mongos is stateless** — it holds no persistent data, only a cached view of
  the config server's chunk metadata; you scale query routing horizontally by
  adding more mongos instances behind a load balancer.
- **Config servers themselves are a replica set** (3 nodes in production) — if
  they're unreachable, existing routing continues to work off cached metadata,
  but chunk splits/migrations and adding new shards will stall.
- **The shard key is immutable for the life of the collection** without an
  explicit `reshardCollection` operation — this is one of the highest-stakes
  early architecture decisions in a sharded system, which is why this project
  dedicates an entire endpoint pair (`/targeted` vs `/scatter-gather`,
  `/explain` vs `/explain/scatter-gather`) to making the cost of getting it
  wrong directly observable.
