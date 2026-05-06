package com.services.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Payload for creating or fully replacing a product.
 *
 * <p>Validation annotations are enforced by Spring (via {@code @Valid}) and produce a 400
 * Bad Request handled by {@code GlobalExceptionHandler}.</p>
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Create / full-replace product payload")
public class ProductRequest {

    @NotBlank
    @Size(min = 1, max = 200)
    @Schema(description = "Display name", example = "Mechanical keyboard")
    private String name;

    @Size(max = 2000)
    @Schema(description = "Marketing description", example = "Tactile, hot-swappable, RGB")
    private String description;

    @NotNull
    @DecimalMin(value = "0.01")
    @Schema(description = "Price in catalogue currency", example = "129.99")
    private BigDecimal price;
}
