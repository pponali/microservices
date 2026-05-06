package com.services.product.service;

import com.services.common.error.exception.ResourceNotFoundException;
import com.services.product.dto.ProductRequest;
import com.services.product.model.Product;
import com.services.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the {@code @Cacheable} / {@code @CacheEvict} wiring on {@link ProductService}.
 *
 * <p>Uses an in-memory {@link ConcurrentMapCacheManager} (overrides the Redis one) so the
 * test is hermetic and runs without external infra. The behavior we're testing — that
 * a second {@code getById} call short-circuits the repo — is identical regardless of the
 * underlying cache backend.</p>
 */
@SpringBootTest(classes = {ProductService.class, ProductServiceCachingTest.TestCacheConfig.class})
@TestPropertySource(properties = "spring.cache.type=simple")
@DirtiesContext
class ProductServiceCachingTest {

    @TestConfiguration
    @EnableCaching
    static class TestCacheConfig {
        @Bean
        @Primary
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("products");
        }
    }

    @Autowired
    private ProductService productService;

    @Autowired
    private CacheManager cacheManager;

    @MockBean
    private ProductRepository productRepository;

    @BeforeEach
    void clearCache() {
        cacheManager.getCacheNames().forEach(n -> cacheManager.getCache(n).clear());
    }

    @Test
    void getById_secondCall_doesNotHitRepository() {
        Product p = Product.builder().id("abc").name("Keyboard").price(BigDecimal.ONE).build();
        when(productRepository.findById("abc")).thenReturn(Optional.of(p));

        productService.getById("abc"); // miss → DB
        productService.getById("abc"); // hit → cache
        productService.getById("abc"); // hit → cache

        verify(productRepository, times(1)).findById("abc");
    }

    @Test
    void getById_missingId_doesNotPoisonCache() {
        // disableCachingNullValues + ResourceNotFoundException combine: nothing gets cached on miss.
        when(productRepository.findById("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getById("nope"))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> productService.getById("nope"))
                .isInstanceOf(ResourceNotFoundException.class);

        // Both calls should have hit the repo — exception path doesn't cache.
        verify(productRepository, times(2)).findById("nope");
    }

    @Test
    void create_evictsCache_soNextReadHitsDb() {
        Product p = Product.builder().id("abc").name("Original").price(BigDecimal.ONE).build();
        when(productRepository.findById("abc")).thenReturn(Optional.of(p));
        when(productRepository.save(org.mockito.ArgumentMatchers.any())).thenReturn(p);

        // Warm cache.
        productService.getById("abc");
        productService.getById("abc");
        verify(productRepository, times(1)).findById("abc");

        // Create evicts allEntries → next getById is a fresh DB load.
        productService.create(ProductRequest.builder().name("New").price(BigDecimal.TEN).build());
        productService.getById("abc");

        verify(productRepository, times(2)).findById("abc");
    }

    @Test
    void delete_evictsTheSpecificKey() {
        Product p = Product.builder().id("abc").name("X").price(BigDecimal.ONE).build();
        when(productRepository.findById("abc")).thenReturn(Optional.of(p));
        when(productRepository.existsById("abc")).thenReturn(true);

        productService.getById("abc");                 // miss → DB
        productService.getById("abc");                 // hit → cache
        verify(productRepository, times(1)).findById("abc");

        productService.delete("abc");

        // After delete, a GET should miss again (the entry was evicted).
        // We don't call getById here because the entity is gone, but we can check the cache directly.
        assertThat(cacheManager.getCache("products").get("abc")).isNull();
    }
}
