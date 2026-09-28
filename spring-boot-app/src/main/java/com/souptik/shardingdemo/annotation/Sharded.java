package com.souptik.shardingdemo.annotation;

import com.souptik.shardingdemo.bootstrap.ShardedEntityInspector;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@code @Document} entity as backing a sharded MongoDB collection
 * and documents the exact shard key used by {@code sh.shardCollection(...)}
 * on the underlying cluster.
 * <p>
 * This annotation is metadata-only: Spring Data MongoDB has no native concept
 * of a "shard key" because sharding is configured at the cluster level (via
 * mongos administrative commands), not the driver/ODM level. Declaring it here
 * gives the codebase a single, discoverable source of truth for:
 * <ul>
 *     <li>Which fields MUST be supplied together to guarantee single-shard routing</li>
 *     <li>Whether the key is ranged or hashed (affects write distribution)</li>
 *     <li>Living documentation that survives refactors, verified at startup by
 *         {@link ShardedEntityInspector}</li>
 * </ul>
 * The actual sharding of the collection is performed once, out-of-band, via
 * {@code scripts/04-add-shards-and-shard-collection.sh} against mongos.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Sharded {

    /**
     * Ordered list of field names composing the shard key, matching the
     * order supplied to {@code sh.shardCollection}. For this project:
     * {@code {"region", "customerId"}} maps to {@code { region: 1, customerId: 1 } }.
     */
    String[] shardKey();

    /**
     * The shard key strategy in effect on the cluster for this collection.
     */
    ShardKeyType type() default ShardKeyType.RANGED;

    enum ShardKeyType {
        /** Range-based partitioning; good for range queries, risk of hot chunks on monotonic keys. */
        RANGED,
        /** Hash of the key value; excellent write distribution, poor for range scans. */
        HASHED
    }
}
