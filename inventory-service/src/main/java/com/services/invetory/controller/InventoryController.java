package com.services.invetory.controller;

import com.services.common.pagination.PageQuery;
import com.services.common.pagination.PagedResponse;
import com.services.invetory.dto.InventoryPatchRequest;
import com.services.invetory.dto.InventoryRequest;
import com.services.invetory.dto.InventoryResponse;
import com.services.invetory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Inventory management REST API.
 *
 * <p>Follows project-wide REST conventions: /api/v1/ URL versioning, full HTTP method
 * coverage, RFC 7807 errors via GlobalExceptionHandler, pagination via PagedResponse,
 * Bean Validation on requests, and OpenAPI annotations on every endpoint.</p>
 *
 * <p>The {@code GET /check} endpoint preserves the original stock-check behaviour
 * (filter by SKU codes) used by order-service.</p>
 */
@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory", description = "Inventory stock management")
public class InventoryController {

    private final InventoryService inventoryService;

    @Operation(summary = "Create a new inventory item")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created"),
            @ApiResponse(responseCode = "400", description = "Validation failed")
    })
    @PostMapping
    public ResponseEntity<InventoryResponse> create(@Valid @RequestBody InventoryRequest request) {
        InventoryResponse created = inventoryService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @Operation(summary = "List inventory items (paginated)")
    @ApiResponse(responseCode = "200", description = "Page of inventory items")
    @GetMapping
    public PagedResponse<InventoryResponse> list(@Valid @ParameterObject PageQuery query) {
        return PagedResponse.of(inventoryService.findAll(query.toPageable()));
    }

    @Operation(summary = "Check stock for one or more SKU codes",
            description = "Returns in-stock status for each provided SKU code.")
    @ApiResponse(responseCode = "200", description = "Stock status list")
    @GetMapping("/check")
    public List<InventoryResponse> isInStock(
            @Parameter(description = "SKU codes to check", required = true)
            @RequestParam List<String> skuCode) {
        return inventoryService.isInStock(skuCode);
    }

    @Operation(summary = "Get inventory item by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Found"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    @GetMapping("/{id}")
    public InventoryResponse getById(
            @Parameter(description = "Inventory item ID") @PathVariable Long id) {
        return inventoryService.getById(id);
    }

    @Operation(summary = "Replace an inventory item (full update)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    @PutMapping("/{id}")
    public InventoryResponse replace(@PathVariable Long id,
                                     @Valid @RequestBody InventoryRequest request) {
        return inventoryService.replace(id, request);
    }

    @Operation(summary = "Patch an inventory item (partial update)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    @PatchMapping("/{id}")
    public InventoryResponse patch(@PathVariable Long id,
                                   @Valid @RequestBody InventoryPatchRequest patch) {
        return inventoryService.patch(id, patch);
    }

    @Operation(summary = "Delete an inventory item")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deleted"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        inventoryService.delete(id);
    }
}
