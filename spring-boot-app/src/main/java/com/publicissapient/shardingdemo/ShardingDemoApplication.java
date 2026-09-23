package com.publicissapient.shardingdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the MongoDB Sharding demonstration service.
 * <p>
 * This application connects exclusively through a {@code mongos} router
 * fronting a two-shard MongoDB cluster. It never opens a direct connection
 * to a shard or config server - all routing decisions (targeted single-shard
 * routing vs. scatter-gather fan-out) are made by mongos itself, based on
 * whether the shard key {@code { region, customerId } } is present in the query.
 */
@SpringBootApplication
public class ShardingDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShardingDemoApplication.class, args);
    }
}
