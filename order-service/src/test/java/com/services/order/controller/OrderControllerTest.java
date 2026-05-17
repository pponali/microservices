package com.services.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.services.common.error.exception.ResourceNotFoundException;
import com.services.order.dto.OrderLineItemsDto;
import com.services.order.dto.OrderLineItemsResponse;
import com.services.order.dto.OrderPatchRequest;
import com.services.order.dto.OrderRequest;
import com.services.order.dto.OrderResponse;
import com.services.order.service.OrderService;
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
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice tests for {@link OrderController}.
 *
 * <p>Verifies HTTP method bindings, status codes, pagination envelope, validation errors
 * (RFC 7807 ProblemDetail), and 404 responses. The Resilience4j stack on POST is
 * bypassed in the slice context (no AOP advisors); the raw CompletableFuture is exercised.</p>
 *
 * <p>{@link com.services.common.error.GlobalExceptionHandler} is imported explicitly so
 * the slice picks it up (auto-config is excluded by default in @WebMvcTest).</p>
 */
@WebMvcTest(controllers = OrderController.class)
@Import(com.services.common.error.GlobalExceptionHandler.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    private static final String BASE = "/api/v1/orders";

    private OrderLineItemsDto validItem() {
        return new OrderLineItemsDto(null, "iphone-13", new BigDecimal("999.99"), 1);
    }

    private OrderResponse sampleResponse(Long id) {
        return OrderResponse.builder()
                .id(id)
                .orderNumber("order-" + id)
                .items(List.of(OrderLineItemsResponse.builder()
                        .id(1L).skuCode("iphone-13")
                        .price(new BigDecimal("999.99")).quantity(1).build()))
                .build();
    }

    @Test
    void placeOrder_returns201() throws Exception {
        OrderRequest req = new OrderRequest(List.of(validItem()));
        when(orderService.placeOrder(any())).thenReturn("Order Placed Successfully");

        MvcResult mvcResult = mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isCreated());
    }

    @Test
    void placeOrder_withInvalidBody_returns400_problemDetail() throws Exception {
        // empty list fails @NotEmpty
        String invalid = """
                {"orderLineItemsDtoList":[]}
                """;

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void list_returnsPagedEnvelope() throws Exception {
        OrderResponse r1 = sampleResponse(1L);
        OrderResponse r2 = sampleResponse(2L);
        Page<OrderResponse> page = new PageImpl<>(List.of(r1, r2), Pageable.ofSize(20), 2);
        when(orderService.findAll(any())).thenReturn(page);

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
        mockMvc.perform(get(BASE).param("size", "10000"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void getById_returns200() throws Exception {
        when(orderService.getById(1L)).thenReturn(sampleResponse(1L));

        mockMvc.perform(get(BASE + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.orderNumber").value("order-1"));
    }

    @Test
    void getById_whenMissing_returns404_problemDetail() throws Exception {
        when(orderService.getById(99L))
                .thenThrow(new ResourceNotFoundException("Order", 99L));

        mockMvc.perform(get(BASE + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.resourceType").value("Order"))
                .andExpect(jsonPath("$.identifier").value(99));
    }

    @Test
    void put_replacesOrder_returns200() throws Exception {
        OrderRequest req = new OrderRequest(List.of(validItem()));
        when(orderService.replaceOrder(eq(1L), any())).thenReturn(sampleResponse(1L));

        mockMvc.perform(put(BASE + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void patch_partialUpdate_returns200() throws Exception {
        OrderPatchRequest patch = new OrderPatchRequest();
        patch.setItems(List.of(validItem()));
        when(orderService.patchOrder(eq(1L), any())).thenReturn(sampleResponse(1L));

        mockMvc.perform(patch(BASE + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patch)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value("order-1"));
    }

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete(BASE + "/1"))
                .andExpect(status().isNoContent());
        verify(orderService).deleteOrder(1L);
    }
}
