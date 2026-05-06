package com.services.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Partial-update payload for {@code PATCH /api/v1/products/{id}}. Every field is optional;
 * unset fields are left unchanged.
 *
 * <p>Use a wrapper class (not a record) so we can distinguish "field absent" from "field set
 * to null" — important for explicit-null PATCH semantics if we ever need them.</p>
 */
@Data
@Schema(description = "Partial product update — only set the fields you want to change.")
public class ProductPatchRequest {

    @Size(min = 1, max = 200)
    @Schema(description = "New display name", example = "Mechanical keyboard v2", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String name;

    @Size(max = 2000)
    @Schema(description = "New description", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String description;

    @DecimalMin(value = "0.01")
    @Schema(description = "New price", example = "139.99", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private BigDecimal price;
}
