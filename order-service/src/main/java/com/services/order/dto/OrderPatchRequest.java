package com.services.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Partial order update — only set the fields you want to change.")
public class OrderPatchRequest {

    @Valid
    @Schema(description = "Replacement line items (replaces entire list when provided)",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private List<OrderLineItemsDto> items;
}
