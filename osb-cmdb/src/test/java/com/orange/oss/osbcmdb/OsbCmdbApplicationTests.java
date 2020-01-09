package com.orange.oss.osbcmdb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import org.springframework.boot.test.context.SpringBootTest;

/**
 * Only run OsbCmdbApplicationTests when system properties are properly defined. Exclude them in circle
 */
@SuppressWarnings("WeakerAccess")
@SpringBootTest
// Expects the Cf client properties to be injected as system properties
// and the corresponding Cf marketplace to be non empty
@EnabledIfSystemProperty(named = "ACCEPTANCE_TEST", matches = "TRUE")
public class OsbCmdbApplicationTests {

    @Test
    public void contextLoads() {
    }

}
