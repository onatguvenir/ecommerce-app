package com.monat.ecommerce.common.dto;
 
import lombok.Builder;
import java.util.List;
 
/**
 * Generic paginated response wrapper.
 * Encourages consistent pagination metadata across all list API endpoints.
 */
@Builder
public record PagedResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last
) {}
