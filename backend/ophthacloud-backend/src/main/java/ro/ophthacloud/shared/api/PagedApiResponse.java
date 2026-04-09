package ro.ophthacloud.shared.api;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Standard paginated collection response wrapper.
 * <p>
 * All endpoints returning a paginated list must use this record.
 * Shape matches GUIDE_04 §1.6:
 * <pre>
 * {
 *   "data": [...],
 *   "pagination": { "page": 0, "size": 20, "totalElements": 187, ... }
 * }
 * </pre>
 *
 * @param <T> the type of each item in the collection
 */
public record PagedApiResponse<T>(List<T> data, PaginationMeta pagination) {

    /**
     * Creates a {@code PagedApiResponse} directly from a Spring Data {@link Page}.
     *
     * @param page the Spring Data page result (already mapped to the DTO type)
     * @param <T>  the DTO type
     * @return a populated paged response
     */
    public static <T> PagedApiResponse<T> of(Page<T> page) {
        return new PagedApiResponse<>(page.getContent(), PaginationMeta.from(page));
    }

    /**
     * Creates a {@code PagedApiResponse} from a raw list and pre-built pagination metadata.
     * Use when the page has been transformed and metadata computed separately.
     */
    public static <T> PagedApiResponse<T> of(List<T> data, PaginationMeta pagination) {
        return new PagedApiResponse<>(data, pagination);
    }
}
