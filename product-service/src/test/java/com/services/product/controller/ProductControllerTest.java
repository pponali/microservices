package com.services.product.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.services.common.error.exception.ResourceNotFoundException;
import com.services.product.dto.ProductRequest;
import com.services.product.dto.ProductResponse;
import com.services.product.service.ProductService;
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

import java.math.BigDecimal;
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
 * Slice tests for {@link ProductController}.
 *
 * <p>Verifies:
 * <ul>
 *   <li>Every HTTP method binds correctly</li>
 *   <li>Status codes match the project conventions (201/204/404/400)</li>
 *   <li>The {@code Location} header is set on POST</li>
 *   <li>Validation errors produce RFC 7807 ProblemDetail responses</li>
 *   <li>{@code ResourceNotFoundException} produces a 404 problem detail</li>
 *   <li>Pagination uses the {@link com.services.common.pagination.PagedResponse} envelope</li>
 * </ul>
 *
 * <p>{@link com.services.common.error.GlobalExceptionHandler} is imported explicitly so
 * the slice picks it up (auto-config is excluded by default in {@code @WebMvcTest}).</p>
 */
@WebMvcTest(controllers = ProductController.class)
@Import(com.services.common.error.GlobalExceptionHandler.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    private static final String BASE = "/api/v1/products";

    @Test
    void create_returns201_withLocationHeader_andBody() throws Exception {
        ProductRequest req = ProductRequest.builder()
                .name("Keyboard")
                .description("Mechanical")
                .price(new BigDecimal("129.99"))
                .build();
        ProductResponse resp = ProductResponse.builder()
                .id("abc-123")
                .name("Keyboard")
                .description("Mechanical")
                .price(new BigDecimal("129.99"))
                .build();
        when(productService.create(any())).thenReturn(resp);

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/v1/products/abc-123")))
                .andExpect(jsonPath("$.id").value("abc-123"))
                .andExpect(jsonPath("$.name").value("Keyboard"));
    }

    @Test
    void create_withInvalidBody_returns400_problemDetail() throws Exception {
        // empty name + null price = 2 violations
        String invalid = """
                {"name":"", "description":"desc", "price":null}
                """;

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
    }

    @Test
    void list_returnsPagedEnvelope() throws Exception {
        ProductResponse one = ProductResponse.builder().id("1").name("A").price(BigDecimal.ONE).build();
        ProductResponse two = ProductResponse.builder().id("2").name("B").price(BigDecimal.TEN).build();
        Page<ProductResponse> page = new PageImpl<>(List.of(one, two), Pageable.ofSize(20), 2);
        when(productService.findAll(any())).thenReturn(page);

        mockMvc.perform(get(BASE).param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void list_withSizeAboveCap_returns400() throws Exception {
        // PageQuery enforces size <= 200
        mockMvc.perform(get(BASE).param("size", "10000"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void getById_whenMissing_returns404_problemDetail() throws Exception {
        when(productService.getById("missing"))
                .thenThrow(new ResourceNotFoundException("Product", "missing"));

        mockMvc.perform(get(BASE + "/missing"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.resourceType").value("Product"))
                .andExpect(jsonPath("$.identifier").value("missing"));
    }

    @Test
    void put_replacesProduct_returns200() throws Exception {
        ProductRequest req = ProductRequest.builder().name("X").price(new BigDecimal("9.99")).build();
        ProductResponse resp = ProductResponse.builder().id("1").name("X").price(new BigDecimal("9.99")).build();
        when(productService.replace(eq("1"), any())).thenReturn(resp);

        mockMvc.perform(put(BASE + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"))
                .andExpect(jsonPath("$.name").value("X"));
    }

    @Test
    void patch_partialUpdate_returns200() throws Exception {
        ProductResponse resp = ProductResponse.builder().id("1").name("Patched").price(BigDecimal.ONE).build();
        when(productService.patch(eq("1"), any())).thenReturn(resp);

        mockMvc.perform(patch(BASE + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Patched\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Patched"));
    }

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete(BASE + "/1"))
                .andExpect(status().isNoContent());
        verify(productService).delete("1");
    }

}
