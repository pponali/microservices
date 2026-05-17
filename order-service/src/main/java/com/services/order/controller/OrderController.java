package com.services.order.controller;

import com.services.common.pagination.PageQuery;
import com.services.common.pagination.PagedResponse;
import com.services.order.dto.OrderPatchRequest;
import com.services.order.dto.OrderRequest;
import com.services.order.dto.OrderResponse;
import com.services.order.service.OrderService;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

/**
 * Order management REST API.
 *
 * <p>Follows project-wide REST conventions (URL versioning, full HTTP method coverage,
 * RFC 7807 errors via GlobalExceptionHandler, pagination, OpenAPI annotations).
 * The POST endpoint keeps its Resilience4j stack (bulkhead, circuit-breaker, time-limiter,
 * retry) for the outbound inventory-service call.</p>
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order management")
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "Place a new order (async, with circuit-breaker on inventory call)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Order accepted for processing"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "503", description = "Inventory service unavailable — try again later")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Bulkhead(name = "inventory")
    @CircuitBreaker(name = "inventory", fallbackMethod = "fallbackMethod")
    @TimeLimiter(name = "inventory")
    @Retry(name = "inventory")
    public CompletableFuture<String> placeOrder(@Valid @RequestBody OrderRequest orderRequest) {
        return CompletableFuture.supplyAsync(() -> orderService.placeOrder(orderRequest));
    }

    public CompletableFuture<String> fallbackMethod(OrderRequest orderRequest, RuntimeException runtimeException) {
        return CompletableFuture.supplyAsync(() -> "Oops! Something went wrong, please order after some time!");
    }

    @Operation(summary = "List orders (paginated)")
    @ApiResponse(responseCode = "200", description = "Page of orders")
    @GetMapping
    public PagedResponse<OrderResponse> list(@Valid @ParameterObject PageQuery query) {
        return PagedResponse.of(orderService.findAll(query.toPageable()));
    }

    @Operation(summary = "Get order by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Found"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    @GetMapping("/{id}")
    public OrderResponse getById(@Parameter(description = "Order ID") @PathVariable Long id) {
        return orderService.getById(id);
    }

    @Operation(summary = "Replace an order (full update)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    @PutMapping("/{id}")
    public OrderResponse replace(@PathVariable Long id, @Valid @RequestBody OrderRequest request) {
        return orderService.replaceOrder(id, request);
    }

    @Operation(summary = "Patch an order (partial update)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    @PatchMapping("/{id}")
    public OrderResponse patch(@PathVariable Long id, @Valid @RequestBody OrderPatchRequest patch) {
        return orderService.patchOrder(id, patch);
    }

    @Operation(summary = "Delete an order")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deleted"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        orderService.deleteOrder(id);
    }
}
