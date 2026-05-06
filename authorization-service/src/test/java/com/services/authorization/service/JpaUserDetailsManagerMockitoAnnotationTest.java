package com.services.authorization.service;

import com.services.authorization.entity.SecurityUser;
import com.services.authorization.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @Author Prakash Ponali (@pponali)
 * @Date 10/12/23
 * @Description
 */
@ExtendWith(MockitoExtension.class)
class JpaUserDetailsManagerMockitoAnnotationTest {

    @Captor
    private ArgumentCaptor<SecurityUser> securityUserArgumentCaptor;

    @Mock
    private JpaUserDetailsManager jpaUserDetailsManager;

    @Mock
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    @DisplayName("This method is used to test the user creation with captor")
    void createUserCaptor() {
        SecurityUser securityUser = new SecurityUser();
        securityUser.setId(123);
        securityUser.setUsername("username");
        securityUser.setEmail("user@gmail.com");
        userRepository.save(securityUser);
        Mockito.verify(userRepository, Mockito.times(1)).save(securityUserArgumentCaptor.capture());
        assertThat(securityUserArgumentCaptor.getValue().getId()).isEqualTo(123);
        assertThat(securityUserArgumentCaptor.getValue().getUsername()).isEqualTo("username");

    }

    @Test
    @DisplayName("This method is used to test the user exist or not.")
    void userExists() {
        assertFalse(jpaUserDetailsManager.userExists("prakash"));
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
       // assertThatThrownBy(() -> jpaUserDetailsManager.userExists("username")).isInstanceOf(UsernameNotFoundException.class).hasMessageMatching("User username not found");
    }

    @Test
    @DisplayName("This method is used to test the user exist or not.")
    void userExistsWithAssert4j() {
        SecurityUser securityUser = new SecurityUser();
        securityUser.setId(123);
        securityUser.setUsername("username");
        securityUser.setEmail("user@gmail.com");
        assertThat(jpaUserDetailsManager.userExists("username")).isFalse();
    }

    @Test
    void loadUserByUsername() {
    }
}