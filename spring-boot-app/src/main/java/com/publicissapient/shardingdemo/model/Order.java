package com.publicissapient.shardingdemo.model;

import com.publicissapient.shardingdemo.annotation.Sharded;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Domain model backing the sharded {@code ecommerce_db.orders} collection.
 * <p>
 * Shard key: {@code { region: 1, customerId: 1 } } (compound, ranged).
 * <p>
 * Every query that supplies BOTH {@code region} and {@code customerId} can be
 * routed by mongos to the single shard owning that chunk range. Any query
 * missing one or both of these fields (e.g. filtering only on {@code status})
 * cannot be targeted and forces mongos into a scatter-gather fan-out across
 * every shard in the cluster.
 */
@Document("orders")
@Sharded(shardKey = {"region", "customerId"}, type = Sharded.ShardKeyType.RANGED)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order implements Serializable {

    @Id
    private String id;

    /**
     * Business-facing order identifier, distinct from the Mongo {@code _id}.
     * Indexed (not part of the shard key) to keep lookups by orderId efficient
     * even though such a lookup, by itself, is still a scatter-gather query.
     */
    @Indexed(unique = true, sparse = true)
    @NotBlank
    private String orderId;

    /**
     * First component of the compound shard key.
     */
    @Field("customerId")
    @NotBlank
    private String customerId;

    /**
     * Second component of the compound shard key. Placed first in the key
     * definition ({@code { region: 1, customerId: 1 } }) so that range queries
     * scoped to a single region can still target a contiguous set of chunks.
     */
    @Field("region")
    @NotBlank
    private String region;

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotBlank
    private String status;

    @CreatedDate
    private Instant createdAt;
}
