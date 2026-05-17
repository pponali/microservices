package com.services.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Create / full-replace order payload")
public class OrderRequest {

    @NotEmpty(message = "Order must contain at least one line item")
    @Valid
    @Schema(description = "Line items (at least one required)")
    private List<OrderLineItemsDto> orderLineItemsDtoList;
}
