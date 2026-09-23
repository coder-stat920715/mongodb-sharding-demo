package com.publicissapient.shardingdemo.bootstrap;

import com.publicissapient.shardingdemo.annotation.Sharded;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Scans the {@code model} package at startup for classes annotated with
 * {@link Sharded} and logs their shard key configuration. This gives every
 * developer (and interviewer) a visible, verifiable confirmation - straight
 * from application logs - of exactly which fields the running code believes
 * constitute the shard key, without needing to inspect the database.
 */
@Slf4j
@Component
public class ShardedEntityInspector implements ApplicationRunner {

    private static final String MODEL_PACKAGE = "com.publicissapient.shardingdemo.model";

    @Override
    public void run(ApplicationArguments args) {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Sharded.class));

        log.info("================================================================");
        log.info(" Sharded entity inventory (package: {})", MODEL_PACKAGE);
        log.info("================================================================");

        for (BeanDefinition bd : scanner.findCandidateComponents(MODEL_PACKAGE)) {
            try {
                Class<?> clazz = Class.forName(bd.getBeanClassName());
                Sharded sharded = clazz.getAnnotation(Sharded.class);
                log.info(" Entity: {}", clazz.getSimpleName());
                log.info("   Shard key : {}", Arrays.toString(sharded.shardKey()));
                log.info("   Key type  : {}", sharded.type());
                log.info("   Rule      : queries supplying ALL key fields -> SINGLE_SHARD routing");
                log.info("               queries missing ANY key field   -> SHARD_MERGE (scatter-gather)");
            } catch (ClassNotFoundException e) {
                log.warn("Could not load candidate class {}", bd.getBeanClassName());
            }
        }
        log.info("================================================================");
    }
}
