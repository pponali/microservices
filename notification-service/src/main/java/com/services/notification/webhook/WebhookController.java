package com.services.notification.webhook;

import com.services.common.error.exception.ResourceNotFoundException;
import com.services.common.pagination.PageQuery;
import com.services.common.pagination.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Tag(name = "Webhooks", description = "Manage outbound webhook subscriptions")
public class WebhookController {

    private final WebhookSubscriptionRepository repository;

    @Operation(summary = "Register a webhook subscription")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Subscription created"),
            @ApiResponse(responseCode = "400", description = "Validation failed")
    })
    @PostMapping
    public ResponseEntity<WebhookSubscriptionResponse> create(@Valid @RequestBody WebhookSubscriptionRequest request) {
        WebhookSubscription sub = WebhookSubscription.builder()
                .eventType(request.getEventType())
                .callbackUrl(request.getCallbackUrl())
                .secret(request.getSecret())
                .enabled(true)
                .createdAt(Instant.now())
                .build();
        WebhookSubscription saved = repository.save(sub);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.getId())
                .toUri();
        return ResponseEntity.created(location).body(WebhookSubscriptionResponse.from(saved));
    }

    @Operation(summary = "List webhook subscriptions (paginated)")
    @GetMapping
    public PagedResponse<WebhookSubscriptionResponse> list(@Valid @ParameterObject PageQuery query) {
        return PagedResponse.of(repository.findAll(query.toPageable()).map(WebhookSubscriptionResponse::from));
    }

    @Operation(summary = "Get one subscription")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Found"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    @GetMapping("/{id}")
    public WebhookSubscriptionResponse getById(@PathVariable UUID id) {
        return repository.findById(id)
                .map(WebhookSubscriptionResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("WebhookSubscription", id));
    }

    @Operation(summary = "Disable a subscription")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Disabled"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("WebhookSubscription", id);
        }
        repository.deleteById(id);
    }
}
