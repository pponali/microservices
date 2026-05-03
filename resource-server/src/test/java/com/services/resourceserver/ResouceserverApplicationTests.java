package com.services.resourceserver;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@EnabledIfSystemProperty(named = "integration.tests", matches = "true")
class ResouceserverApplicationTests {

    @Test
    void contextLoads() {
    }

}
