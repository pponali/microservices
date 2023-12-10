package com.services.authorization.controller;

import com.services.authorization.service.JpaUserDetailsManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @Author Prakash Ponali (@pponali)
 * @Date 10/12/23
 * @Description
 */

@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@WebMvcTest(controllers = SecurityController.class)
class SecurityControllerTest {

    @MockBean
    private JpaUserDetailsManager userDetailsManager;

    @Autowired
    MockMvc mockMvc;

    @BeforeEach
    void setUp() {

    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void index() {
    }

    @Test
    void secure() {
    }

    @Test
    public void getItemByIdTest() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/index")).andExpect(MockMvcResultMatchers.status().is(200));


    }
}