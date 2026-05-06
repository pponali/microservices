package com.services.product.service;

import com.services.common.error.exception.ResourceNotFoundException;
import com.services.product.dto.ProductPatchRequest;
import com.services.product.dto.ProductRequest;
import com.services.product.dto.ProductResponse;
import com.services.product.model.Product;
import com.services.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Caching strategy:
 *
 * <ul>
 *   <li>{@code getById} — {@code @Cacheable("products", key="#id")} — every read is cached for 10 min.</li>
 *   <li>{@code create} — {@code @CacheEvict(allEntries=true)} — invalidates the page-listing entries
 *       (we don't know which pages now contain the new row).</li>
 *   <li>{@code replace} / {@code patch} — {@code @CachePut(key="#id")} — refreshes the entry with the new value
 *       AND {@code @CacheEvict(allEntries=true)} for list pages whose ordering may shift.</li>
 *   <li>{@code delete} — {@code @CacheEvict} on the specific id, plus {@code allEntries=true} for list pages.</li>
 *   <li>{@code findAll} — NOT cached. Caching paginated list responses is anti-pattern (every page+size+sort
 *       combination becomes a separate cache entry, wasting memory and racing with writes).</li>
 * </ul>
 *
 * <p>The {@code @CacheEvict(value="products", allEntries=true)} on writes is broad on purpose:
 * it's the simplest correct strategy. For high-traffic write paths you'd implement a more
 * granular invalidation, but premature optimization here is a footgun.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;

    @CacheEvict(value = "products", allEntries = true)
    public ProductResponse create(ProductRequest request) {
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .build();
        Product saved = productRepository.save(product);
        log.info("Product {} created", saved.getId());
        return toResponse(saved);
    }

    @Cacheable(value = "products", key = "#id")
    public ProductResponse getById(String id) {
        log.debug("Cache miss for product {} — loading from DB", id);
        return productRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }

    public Page<ProductResponse> findAll(Pageable pageable) {
        return productRepository.findAll(pageable).map(this::toResponse);
    }

    @CachePut(value = "products", key = "#id")
    @CacheEvict(value = "products", allEntries = true, condition = "false")
    public ProductResponse replace(String id, ProductRequest request) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        existing.setName(request.getName());
        existing.setDescription(request.getDescription());
        existing.setPrice(request.getPrice());
        return toResponse(productRepository.save(existing));
    }

    @CachePut(value = "products", key = "#id")
    public ProductResponse patch(String id, ProductPatchRequest patch) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        if (patch.getName() != null) existing.setName(patch.getName());
        if (patch.getDescription() != null) existing.setDescription(patch.getDescription());
        if (patch.getPrice() != null) existing.setPrice(patch.getPrice());
        return toResponse(productRepository.save(existing));
    }

    @CacheEvict(value = "products", key = "#id")
    public void delete(String id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product", id);
        }
        productRepository.deleteById(id);
        log.info("Product {} deleted", id);
    }

    private ProductResponse toResponse(Product p) {
        return ProductResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .price(p.getPrice())
                .build();
    }
}
