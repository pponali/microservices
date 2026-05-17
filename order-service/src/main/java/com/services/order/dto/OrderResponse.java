package com.services.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Order response")
public class OrderResponse {

    @Schema(description = "Internal order ID", example = "42")
    private Long id;

    @Schema(description = "Unique order number (UUID)", example = "a3f7-...")
    private String orderNumber;

    @Schema(description = "Line items in this order")
    private List<OrderLineItemsResponse> items;
}
