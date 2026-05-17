package com.services.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Order line item request payload")
public class OrderLineItemsDto {

    @Schema(description = "Internal ID (omit on create)", example = "1")
    private Long id;

    @NotBlank
    @Schema(description = "Stock-keeping unit code", example = "iphone-13")
    private String skuCode;

    @NotNull
    @DecimalMin(value = "0.01")
    @Schema(description = "Unit price", example = "999.99")
    private BigDecimal price;

    @NotNull
    @Min(1)
    @Schema(description = "Quantity (minimum 1)", example = "2")
    private Integer quantity;
}
