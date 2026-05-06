package com.services.common.pagination;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Bind {@code ?page=&size=&sort=name,asc} into a single object — replaces scattered
 * {@code @RequestParam} declarations and gives every paginated endpoint the same surface.
 *
 * <p>Bounds chosen to prevent runaway queries:
 * <ul>
 *   <li>{@code page >= 0}</li>
 *   <li>{@code size} in {@code [1, 200]} — 200 is a hard ceiling regardless of caller request.</li>
 * </ul>
 * </p>
 */
@Schema(description = "Query parameters for paginated endpoints (?page=&size=&sort=).")
public class PageQuery {

    @Min(0)
    @Schema(description = "0-based page index", example = "0", defaultValue = "0")
    private int page = 0;

    @Min(1)
    @Max(200)
    @Schema(description = "Page size, capped at 200", example = "20", defaultValue = "20")
    private int size = 20;

    @Schema(description = "Sort directive: <field>,<asc|desc>. Multiple allowed.", example = "name,asc")
    private String sort;

    public Pageable toPageable() {
        Sort sortSpec = Sort.unsorted();
        if (sort != null && !sort.isBlank()) {
            String[] tokens = sort.split(",");
            String field = tokens[0].trim();
            Sort.Direction dir = Sort.Direction.ASC;
            if (tokens.length > 1 && "desc".equalsIgnoreCase(tokens[1].trim())) {
                dir = Sort.Direction.DESC;
            }
            sortSpec = Sort.by(dir, field);
        }
        return PageRequest.of(page, size, sortSpec);
    }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public String getSort() { return sort; }
    public void setSort(String sort) { this.sort = sort; }
}
