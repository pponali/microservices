package com.services.invetory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "Inventory item response")
public class InventoryResponse {

    @Schema(description = "Internal inventory ID", example = "1")
    private Long id;

    @Schema(description = "Stock-keeping unit code", example = "iphone-13")
    private String skuCode;

    @Schema(description = "Current stock quantity", example = "100")
    private Integer quantity;

    @Schema(description = "True when quantity > 0")
    private boolean isInStock;
}
