package ro.ophthacloud.shared.api;

/**
 * Standard single-resource success response wrapper.
 * <p>
 * All endpoints returning a single resource must wrap their payload in this record.
 * The frontend contract (GUIDE_04 §1.6) expects exactly: {@code { "data": { ... } }}
 *
 * @param <T> the type of the response payload
 */
public record ApiResponse<T>(T data) {

    /** Factory method for cleaner call-site readability. */
    public static <T> ApiResponse<T> of(T data) {
        return new ApiResponse<>(data);
    }
}
