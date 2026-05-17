package com.services.invetory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Create / full-replace inventory item payload")
public class InventoryRequest {

    @NotBlank
    @Size(min = 1, max = 100)
    @Schema(description = "Stock-keeping unit code", example = "iphone-13")
    private String skuCode;

    @NotNull
    @Min(0)
    @Schema(description = "Current stock quantity (0 = out of stock)", example = "100")
    private Integer quantity;
}
