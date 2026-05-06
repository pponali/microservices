package com.services.authorization;

import org.testcontainers.containers.MySQLContainer;

/**
 * @Author Prakash Ponali (@pponali)
 * @Date 10/12/23
 * @Description
 */
public class BaseTest {

    static MySQLContainer<?> mySQLContainer = new MySQLContainer<>("mysql:latest")
            .withDatabaseName("sptools")
            .withDatabaseName("mysql")
            .withPassword("password");
    static {
        mySQLContainer.start();
    }

}
