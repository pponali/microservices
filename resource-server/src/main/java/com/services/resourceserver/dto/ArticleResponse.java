package com.services.resourceserver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Article response")
public class ArticleResponse {

    @Schema(description = "Unique article ID (UUID)", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private String id;

    @Schema(description = "Article title", example = "Introduction to OAuth2")
    private String title;

    @Schema(description = "Article body content")
    private String content;

    @Schema(description = "Author name", example = "Jane Doe")
    private String author;

    @Schema(description = "ISO-8601 creation timestamp", example = "2026-05-17T10:00:00Z")
    private String createdAt;
}
