package com.publicissapient.shardingdemo.service;

import com.mongodb.ExplainVerbosity;
import com.publicissapient.shardingdemo.dto.ExplainSummary;
import com.publicissapient.shardingdemo.model.Order;
import com.publicissapient.shardingdemo.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Contains every query-routing demonstration used by the REST API:
 * <ol>
 *     <li>{@link #createOrder} - targeted write, routed by mongos using the shard key</li>
 *     <li>{@link #findByShardKey} - targeted read, SINGLE_SHARD</li>
 *     <li>{@link #findByStatusScatterGather} - untargeted read, SHARD_MERGE / fan-out</li>
 *     <li>{@link #explainShardKeyQuery} / {@link #explainScatterGatherQuery} - raw explain() plans</li>
 * </ol>
 * All timing is measured with {@link System#nanoTime()} around the actual
 * MongoDB round trip only (not JSON (de)serialization or HTTP overhead), so
 * the latency delta between single-shard and scatter-gather queries observed
 * in the logs is attributable to routing behavior, not framework noise.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final String COLLECTION = "orders";

    private final MongoTemplate mongoTemplate;
    private final OrderRepository orderRepository;

    /**
     * Targeted write. The insert command carries {@code region} and
     * {@code customerId} in the document body; mongos consults the config
     * server's chunk metadata for those values and forwards the insert to
     * the single shard that owns that chunk range - never to both shards.
     */
    public Order createOrder(Order order) {
        if (order.getOrderId() == null || order.getOrderId().isBlank()) {
            order.setOrderId(UUID.randomUUID().toString());
        }
        order.setCreatedAt(Instant.now());

        long start = System.nanoTime();
        Order saved = mongoTemplate.insert(order);
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        log.info("[TARGETED WRITE] orderId={} shardKey=(region={}, customerId={}) inserted in {} ms "
                        + "-> mongos routed this insert to the single shard owning that chunk range",
                saved.getOrderId(), saved.getRegion(), saved.getCustomerId(), elapsedMs);
        return saved;
    }

    /**
     * Single-shard lookup. Both shard key components are supplied, so mongos
     * can prove (via explain) that only one shard was ever contacted.
     */
    public List<Order> findByShardKey(String region, String customerId) {
        Query query = new Query(Criteria.where("region").is(region).and("customerId").is(customerId));

        long start = System.nanoTime();
        List<Order> results = mongoTemplate.find(query, Order.class, COLLECTION);
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        log.info("[SINGLE-SHARD QUERY] region={} customerId={} -> {} doc(s) in {} ms "
                        + "-> shard key fully specified; mongos targeted exactly one shard",
                region, customerId, results.size(), elapsedMs);
        return results;
    }

    /**
     * Scatter-gather anti-pattern demonstration. {@code status} is not part
     * of the shard key, so mongos cannot determine which shard(s) hold
     * matching documents and must broadcast the query to every shard, then
     * merge results before returning them to the client.
     */
    public List<Order> findByStatusScatterGather(String status) {
        Query query = new Query(Criteria.where("status").is(status));

        long start = System.nanoTime();
        List<Order> results = mongoTemplate.find(query, Order.class, COLLECTION);
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        log.warn("[SCATTER-GATHER QUERY] status={} -> {} doc(s) in {} ms "
                        + "-> shard key NOT present; mongos fanned this query out to ALL shards and merged results",
                status, results.size(), elapsedMs);
        return results;
    }

    /**
     * Raw {@code explain()} for the shard-key-targeted query, at
     * EXECUTION_STATS verbosity so {@code executionStats} (nReturned,
     * executionTimeMillis, docsExamined) is populated, not just the plan shape.
     */
    public Document explainShardKeyQuery(String region, String customerId) {
        Document filter = new Document("region", region).append("customerId", customerId);
        return mongoTemplate.getCollection(COLLECTION)
                .find(filter)
                .explain(ExplainVerbosity.EXECUTION_STATS);
    }

    /**
     * Raw {@code explain()} for the untargeted (scatter-gather) query.
     */
    public Document explainScatterGatherQuery(String status) {
        Document filter = new Document("status", status);
        return mongoTemplate.getCollection(COLLECTION)
                .find(filter)
                .explain(ExplainVerbosity.EXECUTION_STATS);
    }

    /**
     * Reduces a raw mongos explain() document down to the fields that
     * actually prove routing behavior: the top-level winning-plan stage
     * ({@code SINGLE_SHARD} vs {@code SHARD_MERGE}), how many shards
     * participated, and basic execution stats.
     */
    @SuppressWarnings("unchecked")
    public ExplainSummary summarize(Document explain) {
        Document queryPlanner = explain.get("queryPlanner", Document.class);
        Document winningPlan = queryPlanner != null ? queryPlanner.get("winningPlan", Document.class) : null;

        String topLevelStage = winningPlan != null ? String.valueOf(winningPlan.get("stage")) : "UNKNOWN";

        List<Document> shardPlans = winningPlan != null
                ? (List<Document>) winningPlan.get("shards")
                : null;

        List<String> shardNames = new ArrayList<>();
        if (shardPlans != null) {
            for (Document shardPlan : shardPlans) {
                shardNames.add(String.valueOf(shardPlan.get("shardName")));
            }
        }

        Document executionStats = explain.get("executionStats", Document.class);

        return ExplainSummary.builder()
                .topLevelStage(topLevelStage)
                .shardsInvolved(shardNames.isEmpty() ? (winningPlan != null ? 1 : 0) : shardNames.size())
                .shardNames(shardNames)
                .nReturned(executionStats != null ? executionStats.get("nReturned") : null)
                .executionTimeMillis(executionStats != null ? executionStats.get("executionTimeMillis") : null)
                .totalKeysExamined(executionStats != null ? executionStats.get("totalKeysExamined") : null)
                .totalDocsExamined(executionStats != null ? executionStats.get("totalDocsExamined") : null)
                .rawExplain(explain)
                .build();
    }

    /**
     * Per-shard document/chunk distribution for {@code ecommerce_db.orders},
     * equivalent to running {@code db.orders.getShardDistribution()} in the
     * mongo shell, surfaced as a REST endpoint for the demo.
     */
    public Document getShardDistribution() {
        return mongoTemplate.getDb().runCommand(new Document("collStats", COLLECTION));
    }

    public List<Order> findAll() {
        return orderRepository.findAll();
    }
}
