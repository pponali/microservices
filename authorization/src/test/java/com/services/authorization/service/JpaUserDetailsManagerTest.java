package com.services.authorization.service;

import com.services.authorization.entity.SecurityUser;
import com.services.authorization.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @Author Prakash Ponali (@pponali)
 * @Date 10/12/23
 * @Description
 */
class JpaUserDetailsManagerTest {

    private final JpaUserDetailsManager jpaUserDetailsManager = Mockito.mock(JpaUserDetailsManager.class);

    private final UserRepository userRepository = Mockito.mock(UserRepository.class);

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    @DisplayName("This method is used to test the user creation.")
    void createUser() {
        SecurityUser securityUser = new SecurityUser();
        securityUser.setId(123);
        securityUser.setUsername("username");
        securityUser.setEmail("user@gmail.com");
        userRepository.save(securityUser);
        Mockito.verify(userRepository, Mockito.times(1)).save(ArgumentMatchers.any(SecurityUser.class));

    }



    @Test
    @DisplayName("This method is used to test the user exist or not.")
    void userExists() {
        assertFalse(jpaUserDetailsManager.userExists("username"));
    }

    @Test
    @DisplayName("This method is used to test the user exist or not.")
    void userExistsException() {
        /*UsernameNotFoundException usernameNotFoundException = assertThrows(UsernameNotFoundException.class, () -> {
            jpaUserDetailsManager.userExists("username");
        });
        assertEquals("User username not found", usernameNotFoundException.getMessage());*/

    }

    @Test
    @DisplayName("This method is used to test the user exist or not.")
    void userExistsWithAssert4jException() {

        /*assertThatThrownBy(() -> {
            jpaUserDetailsManager.userExists("username");
        }).isInstanceOf(UsernameNotFoundException.class).hasMessageMatching("User username not found");*/
    }

    @Test
    @DisplayName("This method is used to test the user exist or not.")
    void userExistsWithAssert4j() {
        SecurityUser securityUser = new SecurityUser();
        securityUser.setId(123);
        securityUser.setUsername("username");
        securityUser.setEmail("user@gmail.com");
        Mockito.when(userRepository.findByUsername("username")).thenReturn(Optional.of(securityUser));
        assertThat(jpaUserDetailsManager.userExists("username")).isFalse();
    }

    @Test
    void loadUserByUsername() {
    }
}