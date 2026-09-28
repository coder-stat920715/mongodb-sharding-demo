package com.souptik.shardingdemo.controller;

import com.souptik.shardingdemo.dto.ExplainSummary;
import com.souptik.shardingdemo.model.Order;
import com.souptik.shardingdemo.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST surface for the NoSQL sharding demonstration. Each endpoint is
 * intentionally paired with a routing behavior called out in its Javadoc so
 * the mapping between "what the client asked for" and "what mongos actually
 * did" is explicit and easy to narrate in an interview / demo setting.
 */
@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * Targeted write: POST /api/orders
     * Body must include region + customerId (the shard key). mongos routes
     * the insert to the single shard owning that key range.
     */
    @PostMapping
    public ResponseEntity<Order> createOrder(@Valid @RequestBody Order order) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(order));
    }

    /**
     * Single-shard lookup: GET /api/orders/targeted?region=US&customerId=C123
     * Full shard key supplied -> SINGLE_SHARD routing.
     */
    @GetMapping("/targeted")
    public ResponseEntity<List<Order>> targeted(
            @RequestParam String region,
            @RequestParam String customerId) {
        return ResponseEntity.ok(orderService.findByShardKey(region, customerId));
    }

    /**
     * Scatter-gather anti-pattern: GET /api/orders/scatter-gather?status=PENDING
     * Shard key absent -> mongos fans the query out to every shard.
     */
    @GetMapping("/scatter-gather")
    public ResponseEntity<List<Order>> scatterGather(@RequestParam String status) {
        return ResponseEntity.ok(orderService.findByStatusScatterGather(status));
    }

    /**
     * Explain plan for the targeted (shard-key) query.
     * GET /api/orders/explain?region=US&customerId=C123
     * Expect {@code topLevelStage = "SINGLE_SHARD"}.
     */
    @GetMapping("/explain")
    public ResponseEntity<ExplainSummary> explainTargeted(
            @RequestParam String region,
            @RequestParam String customerId) {
        Document explain = orderService.explainShardKeyQuery(region, customerId);
        return ResponseEntity.ok(orderService.summarize(explain));
    }

    /**
     * Explain plan for the scatter-gather query.
     * GET /api/orders/explain/scatter-gather?status=PENDING
     * Expect {@code topLevelStage = "SHARD_MERGE"} with multiple shard names listed.
     */
    @GetMapping("/explain/scatter-gather")
    public ResponseEntity<ExplainSummary> explainScatterGather(@RequestParam String status) {
        Document explain = orderService.explainScatterGatherQuery(status);
        return ResponseEntity.ok(orderService.summarize(explain));
    }

    /**
     * Per-shard collection statistics for ecommerce_db.orders (document
     * counts, storage size per shard, etc.) - shows the physical data
     * distribution resulting from the shard key choice.
     * GET /api/orders/shard-distribution
     */
    @GetMapping("/shard-distribution")
    public ResponseEntity<Document> shardDistribution() {
        return ResponseEntity.ok(orderService.getShardDistribution());
    }

    /**
     * Convenience endpoint to list everything (itself a scatter-gather query,
     * since no filter is applied at all) - useful for seeding/demo sanity checks.
     * GET /api/orders
     */
    @GetMapping
    public ResponseEntity<List<Order>> findAll() {
        return ResponseEntity.ok(orderService.findAll());
    }
}
