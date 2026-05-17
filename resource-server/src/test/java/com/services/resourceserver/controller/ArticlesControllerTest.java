package com.services.resourceserver.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.services.common.error.exception.ResourceNotFoundException;
import com.services.resourceserver.config.ResourceServerConfig;
import com.services.resourceserver.dto.ArticlePatchRequest;
import com.services.resourceserver.dto.ArticleRequest;
import com.services.resourceserver.dto.ArticleResponse;
import com.services.resourceserver.service.ArticleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice tests for {@link ArticlesController}.
 *
 * <p>All requests require a JWT bearer token (enforced by SecurityConfig's filter chain).
 * Tests use Spring Security Test's {@code jwt()} post-processor to inject a synthetic JWT
 * into the security context without hitting any real authorization server.</p>
 *
 * <p>{@link ResourceServerConfig} is excluded from the slice because it injects
 * {@code OAuth2AuthorizationServerProperties} which is not available in the test context.</p>
 *
 * <p>{@link com.services.common.error.GlobalExceptionHandler} is imported explicitly so
 * the slice picks it up (auto-config is excluded by default in @WebMvcTest).</p>
 */
@WebMvcTest(
        controllers = ArticlesController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = ResourceServerConfig.class
        )
)
@Import(com.services.common.error.GlobalExceptionHandler.class)
class ArticlesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ArticleService articleService;

    @MockBean
    private JwtDecoder jwtDecoder;

    private static final String BASE = "/api/v1/articles";

    private ArticleResponse sample(String id) {
        return ArticleResponse.builder()
                .id(id)
                .title("OAuth2 Deep Dive")
                .content("Content here...")
                .author("Jane Doe")
                .createdAt("2026-05-17T10:00:00Z")
                .build();
    }

    @Test
    void create_returns201_withLocationHeader_andBody() throws Exception {
        ArticleRequest req = new ArticleRequest("OAuth2 Deep Dive", "Content here...", "Jane Doe");
        when(articleService.create(any())).thenReturn(sample("abc-123"));

        mockMvc.perform(post(BASE).with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.endsWith("/api/v1/articles/abc-123")))
                .andExpect(jsonPath("$.id").value("abc-123"))
                .andExpect(jsonPath("$.title").value("OAuth2 Deep Dive"));
    }

    @Test
    void create_withInvalidBody_returns400_problemDetail() throws Exception {
        // blank title and author — 2 violations
        String invalid = """
                {"title":"", "content":"Some content", "author":""}
                """;

        mockMvc.perform(post(BASE).with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors.length()").value(
                        org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
    }

    @Test
    void create_withoutToken_returns4xx() throws Exception {
        // Spring Security rejects unauthenticated requests; the exact code (401 or 403)
        // depends on CSRF filter ordering — either way it's a client error.
        ArticleRequest req = new ArticleRequest("Title", "Content", "Author");

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void list_returnsPagedEnvelope() throws Exception {
        Page<ArticleResponse> page = new PageImpl<>(
                List.of(sample("1"), sample("2")), Pageable.ofSize(20), 2);
        when(articleService.findAll(any())).thenReturn(page);

        mockMvc.perform(get(BASE).with(jwt()).param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void getById_whenMissing_returns404_problemDetail() throws Exception {
        when(articleService.getById("missing"))
                .thenThrow(new ResourceNotFoundException("Article", "missing"));

        mockMvc.perform(get(BASE + "/missing").with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.resourceType").value("Article"))
                .andExpect(jsonPath("$.identifier").value("missing"));
    }

    @Test
    void put_replacesArticle_returns200() throws Exception {
        ArticleRequest req = new ArticleRequest("Updated Title", "Updated content", "John");
        ArticleResponse resp = ArticleResponse.builder()
                .id("abc-123").title("Updated Title")
                .content("Updated content").author("John")
                .createdAt("2026-05-17T10:00:00Z").build();
        when(articleService.replace(eq("abc-123"), any())).thenReturn(resp);

        mockMvc.perform(put(BASE + "/abc-123").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"));
    }

    @Test
    void patch_partialUpdate_returns200() throws Exception {
        ArticlePatchRequest patchReq = new ArticlePatchRequest();
        patchReq.setTitle("Patched Title");
        ArticleResponse resp = sample("abc-123");
        resp.setTitle("Patched Title");
        when(articleService.patch(eq("abc-123"), any())).thenReturn(resp);

        mockMvc.perform(patch(BASE + "/abc-123").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patchReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Patched Title"));
    }

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete(BASE + "/abc-123").with(jwt()))
                .andExpect(status().isNoContent());
        verify(articleService).delete("abc-123");
    }
}
