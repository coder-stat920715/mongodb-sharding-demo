package com.publicissapient.shardingdemo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * Enables {@code @CreatedDate} / {@code @LastModifiedDate} auditing support.
 * <p>
 * No custom {@link org.springframework.data.mongodb.core.MongoTemplate} bean
 * is declared here - Spring Boot's auto-configuration builds one from the
 * {@code spring.data.mongodb.uri} property in application.yml, which points
 * at the mongos router. That single template is reused by the service layer
 * for both simple CRUD and raw {@code explain()} diagnostics.
 */
@Configuration
@EnableMongoAuditing
public class MongoConfig {
}
