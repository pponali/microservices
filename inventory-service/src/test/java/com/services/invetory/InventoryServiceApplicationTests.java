package com.services.invetory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@EnabledIfSystemProperty(named = "integration.tests", matches = "true")
class InventoryServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
