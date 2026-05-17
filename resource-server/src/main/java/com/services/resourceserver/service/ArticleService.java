package com.services.resourceserver.service;

import com.services.common.error.exception.ResourceNotFoundException;
import com.services.resourceserver.dto.ArticlePatchRequest;
import com.services.resourceserver.dto.ArticleRequest;
import com.services.resourceserver.dto.ArticleResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory article store demonstrating service-layer patterns for resource-server.
 * A real implementation would back this with a JPA repository.
 */
@Service
public class ArticleService {

    private final Map<String, ArticleResponse> store = new ConcurrentHashMap<>();

    public ArticleResponse create(ArticleRequest request) {
        String id = UUID.randomUUID().toString();
        ArticleResponse article = ArticleResponse.builder()
                .id(id)
                .title(request.getTitle())
                .content(request.getContent())
                .author(request.getAuthor())
                .createdAt(Instant.now().toString())
                .build();
        store.put(id, article);
        return article;
    }

    public Page<ArticleResponse> findAll(Pageable pageable) {
        List<ArticleResponse> all = new ArrayList<>(store.values());
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), all.size());
        List<ArticleResponse> slice = start >= all.size() ? List.of() : all.subList(start, end);
        return new PageImpl<>(slice, pageable, all.size());
    }

    public ArticleResponse getById(String id) {
        ArticleResponse article = store.get(id);
        if (article == null) {
            throw new ResourceNotFoundException("Article", id);
        }
        return article;
    }

    public ArticleResponse replace(String id, ArticleRequest request) {
        if (!store.containsKey(id)) {
            throw new ResourceNotFoundException("Article", id);
        }
        ArticleResponse existing = store.get(id);
        ArticleResponse updated = ArticleResponse.builder()
                .id(id)
                .title(request.getTitle())
                .content(request.getContent())
                .author(request.getAuthor())
                .createdAt(existing.getCreatedAt())
                .build();
        store.put(id, updated);
        return updated;
    }

    public ArticleResponse patch(String id, ArticlePatchRequest patch) {
        ArticleResponse existing = getById(id);
        ArticleResponse updated = ArticleResponse.builder()
                .id(id)
                .title(patch.getTitle() != null ? patch.getTitle() : existing.getTitle())
                .content(patch.getContent() != null ? patch.getContent() : existing.getContent())
                .author(patch.getAuthor() != null ? patch.getAuthor() : existing.getAuthor())
                .createdAt(existing.getCreatedAt())
                .build();
        store.put(id, updated);
        return updated;
    }

    public void delete(String id) {
        if (!store.containsKey(id)) {
            throw new ResourceNotFoundException("Article", id);
        }
        store.remove(id);
    }
}
