package com.services.resourceserver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Create / full-replace article payload")
public class ArticleRequest {

    @NotBlank
    @Size(min = 1, max = 200)
    @Schema(description = "Article title", example = "Introduction to OAuth2")
    private String title;

    @NotBlank
    @Size(min = 1, max = 10000)
    @Schema(description = "Article body content")
    private String content;

    @NotBlank
    @Size(min = 1, max = 100)
    @Schema(description = "Author name", example = "Jane Doe")
    private String author;
}
