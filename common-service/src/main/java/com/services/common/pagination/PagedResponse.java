package com.services.common.pagination;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Stable, serialization-friendly page envelope used across every paginated endpoint.
 *
 * <p>Spring Data's own {@code Page} JSON shape is unstable across versions and includes
 * fields callers don't need ({@code pageable}, {@code sort}, {@code unsorted} etc.).
 * Wrapping it in this record gives clients a contract that won't drift.</p>
 *
 * @param content    the rows on this page
 * @param page       0-based page number
 * @param size       requested page size
 * @param totalElements total rows across all pages
 * @param totalPages number of pages given the size
 * @param first      true if this is the first page
 * @param last       true if this is the last page
 */
@Schema(description = "Standard page envelope returned by every paginated endpoint.")
public record PagedResponse<T>(
        @Schema(description = "Rows on this page (already DTO-mapped).")
        List<T> content,
        @Schema(description = "0-based page index", example = "0")
        int page,
        @Schema(description = "Requested page size", example = "20")
        int size,
        @Schema(description = "Total elements across all pages", example = "137")
        long totalElements,
        @Schema(description = "Total number of pages", example = "7")
        int totalPages,
        @Schema(description = "True if this is the first page")
        boolean first,
        @Schema(description = "True if this is the last page")
        boolean last
) {

    /** Build a {@link PagedResponse} from an already-mapped Spring Data {@link Page}. */
    public static <T> PagedResponse<T> of(Page<T> page) {
        return new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
