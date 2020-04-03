package com.orange.oss.osbcmdb;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;

/**
 * Only run OsbCmdbApplicationTests when system properties are properly defined. Exclude them in circle
 */
@SuppressWarnings("WeakerAccess")
@SpringBootTest
// Expects the Cf client properties to be injected as system properties
// and the corresponding Cf marketplace to be non empty
@Tag("ExpectsProdProperties")
@Tag("AcceptanceTest")
public class OsbCmdbApplicationTests {

    @Test
    public void contextLoads() {
    }

}
