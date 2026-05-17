package com.services.invetory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.services.common.error.exception.ResourceNotFoundException;
import com.services.invetory.dto.InventoryPatchRequest;
import com.services.invetory.dto.InventoryRequest;
import com.services.invetory.dto.InventoryResponse;
import com.services.invetory.repository.InventoryRepository;
import com.services.invetory.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice tests for {@link InventoryController}.
 *
 * <p>Verifies: HTTP method bindings, status codes (201/204/404/400), Location header on POST,
 * RFC 7807 ProblemDetail on errors, paginated envelope on GET list, and stock-check endpoint.</p>
 *
 * <p>{@link com.services.common.error.GlobalExceptionHandler} is imported explicitly so
 * the slice picks it up (auto-config is excluded by default in @WebMvcTest).</p>
 */
@WebMvcTest(controllers = InventoryController.class)
@Import(com.services.common.error.GlobalExceptionHandler.class)
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InventoryService inventoryService;

    // The @SpringBootApplication class has a CommandLineRunner @Bean that injects
    // InventoryRepository. @WebMvcTest doesn't load JPA, so we mock the repo here
    // to satisfy that dependency without pulling in a real datasource.
    @MockBean
    private InventoryRepository inventoryRepository;

    private static final String BASE = "/api/v1/inventory";

    private InventoryResponse sample(Long id) {
        return InventoryResponse.builder()
                .id(id).skuCode("iphone-13").quantity(100).isInStock(true).build();
    }

    @Test
    void create_returns201_withLocationHeader_andBody() throws Exception {
        InventoryRequest req = new InventoryRequest("iphone-13", 100);
        when(inventoryService.create(any())).thenReturn(sample(1L));

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.endsWith("/api/v1/inventory/1")))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.skuCode").value("iphone-13"));
    }

    @Test
    void create_withInvalidBody_returns400_problemDetail() throws Exception {
        // blank skuCode and negative quantity
        String invalid = """
                {"skuCode":"", "quantity":-5}
                """;

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors.length()").value(
                        org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
    }

    @Test
    void list_returnsPagedEnvelope() throws Exception {
        Page<InventoryResponse> page =
                new PageImpl<>(List.of(sample(1L), sample(2L)), Pageable.ofSize(20), 2);
        when(inventoryService.findAll(any())).thenReturn(page);

        mockMvc.perform(get(BASE).param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void check_returnsStockList() throws Exception {
        when(inventoryService.isInStock(List.of("iphone-13"))).thenReturn(List.of(sample(1L)));

        // Jackson strips the `is` prefix from boolean getters: isInStock() → JSON key "inStock"
        mockMvc.perform(get(BASE + "/check").param("skuCode", "iphone-13"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].skuCode").value("iphone-13"))
                .andExpect(jsonPath("$[0].inStock").value(true));
    }

    @Test
    void getById_whenMissing_returns404_problemDetail() throws Exception {
        when(inventoryService.getById(99L))
                .thenThrow(new ResourceNotFoundException("Inventory", 99L));

        mockMvc.perform(get(BASE + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.resourceType").value("Inventory"))
                .andExpect(jsonPath("$.identifier").value(99));
    }

    @Test
    void put_replacesItem_returns200() throws Exception {
        InventoryRequest req = new InventoryRequest("iphone-13-pro", 50);
        InventoryResponse resp = InventoryResponse.builder()
                .id(1L).skuCode("iphone-13-pro").quantity(50).isInStock(true).build();
        when(inventoryService.replace(eq(1L), any())).thenReturn(resp);

        mockMvc.perform(put(BASE + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skuCode").value("iphone-13-pro"))
                .andExpect(jsonPath("$.quantity").value(50));
    }

    @Test
    void patch_partialUpdate_returns200() throws Exception {
        InventoryPatchRequest patchReq = new InventoryPatchRequest();
        patchReq.setQuantity(200);
        InventoryResponse resp = InventoryResponse.builder()
                .id(1L).skuCode("iphone-13").quantity(200).isInStock(true).build();
        when(inventoryService.patch(eq(1L), any())).thenReturn(resp);

        mockMvc.perform(patch(BASE + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patchReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(200));
    }

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete(BASE + "/1"))
                .andExpect(status().isNoContent());
        verify(inventoryService).delete(1L);
    }
}
