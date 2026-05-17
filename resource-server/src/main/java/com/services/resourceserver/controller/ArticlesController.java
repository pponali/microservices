package com.services.resourceserver.controller;

import com.services.common.pagination.PageQuery;
import com.services.common.pagination.PagedResponse;
import com.services.resourceserver.dto.ArticlePatchRequest;
import com.services.resourceserver.dto.ArticleRequest;
import com.services.resourceserver.dto.ArticleResponse;
import com.services.resourceserver.service.ArticleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
 * Articles REST API — secured by OAuth2 JWT bearer token.
 *
 * <p>Follows project-wide REST conventions: /api/v1/ URL versioning, full HTTP method
 * coverage, RFC 7807 errors via GlobalExceptionHandler, pagination via PagedResponse,
 * Bean Validation on requests, and OpenAPI annotations on every endpoint.</p>
 *
 * <p>All endpoints require a valid JWT bearer token (enforced by the security filter chain).
 * No additional {@code @PreAuthorize} constraints are applied at the method level
 * beyond the global {@code anyRequest().authenticated()} rule.</p>
 */
@RestController
@RequestMapping("/api/v1/articles")
@RequiredArgsConstructor
@Tag(name = "Articles", description = "Article management (JWT bearer token required)")
@SecurityRequirement(name = "bearerAuth")
public class ArticlesController {

    private final ArticleService articleService;

    @Operation(summary = "Create a new article")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Unauthorised — bearer token required")
    })
    @PostMapping
    public ResponseEntity<ArticleResponse> create(@Valid @RequestBody ArticleRequest request) {
        ArticleResponse created = articleService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @Operation(summary = "List articles (paginated)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of articles"),
            @ApiResponse(responseCode = "401", description = "Unauthorised")
    })
    @GetMapping
    public PagedResponse<ArticleResponse> list(@Valid @ParameterObject PageQuery query) {
        return PagedResponse.of(articleService.findAll(query.toPageable()));
    }

    @Operation(summary = "Get article by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Found"),
            @ApiResponse(responseCode = "401", description = "Unauthorised"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    @GetMapping("/{id}")
    public ArticleResponse getById(
            @Parameter(description = "Article UUID") @PathVariable String id) {
        return articleService.getById(id);
    }

    @Operation(summary = "Replace an article (full update)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Unauthorised"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    @PutMapping("/{id}")
    public ArticleResponse replace(@PathVariable String id,
                                   @Valid @RequestBody ArticleRequest request) {
        return articleService.replace(id, request);
    }

    @Operation(summary = "Patch an article (partial update)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated"),
            @ApiResponse(responseCode = "401", description = "Unauthorised"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    @PatchMapping("/{id}")
    public ArticleResponse patch(@PathVariable String id,
                                 @Valid @RequestBody ArticlePatchRequest patch) {
        return articleService.patch(id, patch);
    }

    @Operation(summary = "Delete an article")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deleted"),
            @ApiResponse(responseCode = "401", description = "Unauthorised"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        articleService.delete(id);
    }
}
