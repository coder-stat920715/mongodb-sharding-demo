//package com.souptik.shardingdemo;
//
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
//import org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration;
//import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.context.ApplicationContext;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
///**
// * Plain context-load smoke test. MongoDB autoconfiguration is excluded here
// * so the build (mvn clean install) never needs a live or embedded database -
// * it only proves the Spring context wires up correctly (bean definitions,
// * component scanning, property binding, etc.).
// * <p>
// * Sharding-specific behavior (SINGLE_SHARD vs SHARD_MERGE routing) can only
// * be verified against the real multi-shard cluster: start it with
// * scripts/init-sharded-cluster.sh, run the app normally (this exclusion does
// * NOT apply at runtime, only in this test), then exercise the
// * /api/orders/explain and /api/orders/explain/scatter-gather endpoints.
// */
//@SpringBootTest(
//        properties = "spring.autoconfigure.exclude="
//                + "org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,"
//                + "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration,"
//                + "org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration"
//)
//class ShardingDemoApplicationTests {
//
//    @Autowired
//    private ApplicationContext applicationContext;
//
//    @Test
//    void contextLoads() {
//        assertThat(applicationContext).isNotNull();
//    }
//}
