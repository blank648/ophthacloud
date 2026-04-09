package ro.ophthacloud.shared.api;

/**
 * Pagination metadata included in every paginated collection response.
 * <p>
 * Matches the {@code "pagination"} block in GUIDE_04 §1.6:
 * <pre>
 * {
 *   "data": [...],
 *   "pagination": {
 *     "page": 0,
 *     "size": 20,
 *     "totalElements": 187,
 *     "totalPages": 10,
 *     "hasNext": true,
 *     "hasPrevious": false
 *   }
 * }
 * </pre>
 */
public record PaginationMeta(
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious
) {
    /**
     * Creates a {@code PaginationMeta} from a Spring Data {@link org.springframework.data.domain.Page}.
     *
     * @param springPage the Spring Data page result
     * @return a populated {@code PaginationMeta} record
     */
    public static PaginationMeta from(org.springframework.data.domain.Page<?> springPage) {
        return new PaginationMeta(
                springPage.getNumber(),
                springPage.getSize(),
                springPage.getTotalElements(),
                springPage.getTotalPages(),
                springPage.hasNext(),
                springPage.hasPrevious()
        );
    }
}
