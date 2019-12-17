package com.orange.oss.osbcmdb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import org.springframework.boot.test.context.SpringBootTest;

/**
 * Only run OsbCmdbApplicationTests when system properties are properly defined. Exclude them in circle
 */
@SuppressWarnings("WeakerAccess")
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "acceptanceTests", matches = "true")
public class OsbCmdbApplicationTests {

    @Test
    public void contextLoads() {
    }

}
