package com.services.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Order line item in a response")
public class OrderLineItemsResponse {

    @Schema(description = "Line item ID", example = "1")
    private Long id;

    @Schema(description = "Stock-keeping unit code", example = "iphone-13")
    private String skuCode;

    @Schema(description = "Unit price", example = "999.99")
    private BigDecimal price;

    @Schema(description = "Quantity ordered", example = "2")
    private Integer quantity;
}
