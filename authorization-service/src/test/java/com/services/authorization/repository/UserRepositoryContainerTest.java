package com.services.authorization.repository;

import com.services.authorization.BaseTest;
import com.services.authorization.entity.SecurityUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @Author Prakash Ponali (@pponali)
 * @Date 10/12/23
 * @Description
 */

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryContainerTest extends BaseTest {

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void deleteInBatch() {
    }

    @Test
    void findByUsername() {
    }



    @Test
    void saveUser(){
        SecurityUser user = new SecurityUser(234345, "test user",  "secret password",null, true, true,  true, true, null);
        SecurityUser securityUser = userRepository.save(user);
        assertThat(securityUser).usingRecursiveComparison().ignoringFields("id").isEqualTo(user);
    }
}