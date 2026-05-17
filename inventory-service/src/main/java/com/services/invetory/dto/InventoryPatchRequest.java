package com.services.invetory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Partial inventory update — only set the fields you want to change.")
public class InventoryPatchRequest {

    @Size(min = 1, max = 100)
    @Schema(description = "New SKU code", example = "iphone-13-pro",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String skuCode;

    @Min(0)
    @Schema(description = "New stock quantity", example = "50",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Integer quantity;
}
