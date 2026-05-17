package com.services.resourceserver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Partial article update — only set the fields you want to change.")
public class ArticlePatchRequest {

    @Size(min = 1, max = 200)
    @Schema(description = "New article title", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String title;

    @Size(min = 1, max = 10000)
    @Schema(description = "New article body content", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String content;

    @Size(min = 1, max = 100)
    @Schema(description = "New author name", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String author;
}
