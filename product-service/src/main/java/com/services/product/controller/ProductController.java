package com.services.product.controller;

import com.services.common.pagination.PageQuery;
import com.services.common.pagination.PagedResponse;
import com.services.product.dto.ProductPatchRequest;
import com.services.product.dto.ProductRequest;
import com.services.product.dto.ProductResponse;
import com.services.product.service.ProductService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * Product catalogue REST API.
 *
 * <p>Demonstrates the project-wide REST conventions:
 * <ul>
 *   <li><strong>API versioning</strong>: every endpoint lives under {@code /api/v1/}</li>
 *   <li><strong>HTTP method coverage</strong>: GET (collection + item), POST (create), PUT (replace), PATCH (partial), DELETE</li>
 *   <li><strong>Status codes</strong>: 200 (read), 201 + Location (create), 204 (delete), 404 / 400 / 409 via {@code GlobalExceptionHandler}</li>
 *   <li><strong>Pagination</strong>: {@link PageQuery} bound from query string, {@link PagedResponse} envelope</li>
 *   <li><strong>OpenAPI</strong>: every operation is annotated, every status code is documented</li>
 * </ul>
 * </p>
 */
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product catalogue management")
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "Create a new product")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "409", description = "Duplicate")
    })
    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        ProductResponse created = productService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @Operation(summary = "List products (paginated)")
    @ApiResponse(responseCode = "200", description = "Page of products")
    @GetMapping
    public PagedResponse<ProductResponse> list(@Valid @ParameterObject PageQuery query) {
        return PagedResponse.of(productService.findAll(query.toPageable()));
    }

    @Operation(summary = "Get one product by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Found"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    @GetMapping("/{id}")
    public ProductResponse getById(@Parameter(description = "Product id") @PathVariable String id) {
        return productService.getById(id);
    }

    @Operation(summary = "Replace a product (full update)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    @PutMapping("/{id}")
    public ProductResponse replace(@PathVariable String id, @Valid @RequestBody ProductRequest request) {
        return productService.replace(id, request);
    }

    @Operation(summary = "Patch a product (partial update)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    @PatchMapping("/{id}")
    public ProductResponse patch(@PathVariable String id, @Valid @RequestBody ProductPatchRequest patch) {
        return productService.patch(id, patch);
    }

    @Operation(summary = "Delete a product")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deleted"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        productService.delete(id);
    }
}
