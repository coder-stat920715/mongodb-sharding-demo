package com.souptik.shardingdemo.repository;

import com.souptik.shardingdemo.model.Order;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface OrderRepository extends MongoRepository<Order, String> {

    /**
     * Query that supplies the FULL shard key. mongos can route this directly
     * to the single shard owning the {region, customerId} chunk range.
     */
    List<Order> findByRegionAndCustomerId(String region, String customerId);

    /**
     * Query that does NOT include the shard key. mongos must broadcast this
     * to every shard in the cluster and merge the results (scatter-gather).
     */
    List<Order> findByStatus(String status);

    /**
     * Lookup by business orderId only - also NOT shard-key-targeted, since
     * orderId is not part of the shard key, so this is scatter-gather too
     * despite the field being uniquely indexed on each shard.
     */
    Order findByOrderId(String orderId);
}
