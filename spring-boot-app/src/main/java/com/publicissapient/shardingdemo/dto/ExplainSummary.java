package com.publicissapient.shardingdemo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import org.bson.Document;

import java.util.List;

/**
 * Human-readable digest of a mongos {@code explain()} response, pulling out
 * exactly the fields that matter for demonstrating query routing behavior:
 * <ul>
 *     <li>{@code topLevelStage} - {@code SINGLE_SHARD} for a targeted query,
 *         {@code SHARD_MERGE} for a scatter-gather query</li>
 *     <li>{@code shardsInvolved} / {@code shardNames} - how many, and which,
 *         shards actually executed the query</li>
 *     <li>execution stats - documents examined/returned and latency</li>
 * </ul>
 * The full raw explain document is also included for anyone who wants to dig
 * into {@code winningPlan.shards[].winningPlan} per-shard plan details.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExplainSummary {

    private String topLevelStage;
    private int shardsInvolved;
    private List<String> shardNames;
    private Object nReturned;
    private Object executionTimeMillis;
    private Object totalKeysExamined;
    private Object totalDocsExamined;
    private Document rawExplain;
}
