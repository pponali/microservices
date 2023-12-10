package com.services.authorization.controller;

import com.services.authorization.service.JpaUserDetailsManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @Author Prakash Ponali (@pponali)
 * @Date 10/12/23
 * @Description
 */

@ExtendWith(MockitoExtension.class)
class SecurityControllerTest {

    private MockMvc mockMvc;

    @Mock
    private JpaUserDetailsManager itemService;

    @InjectMocks
    private SecurityController securityController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(securityController).build();
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
        Long itemId = 1L;
        //Item mockItem = new Item(itemId, "ItemName", "Description", 9.99);
        //when(itemService.findById(itemId)).thenReturn(Optional.of(mockItem));

        mockMvc.perform(get("/api/items/" + itemId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(itemId))
                .andExpect(jsonPath("$.name").value("ItemName"));

        //verify(itemService).findById(itemId);
    }
}