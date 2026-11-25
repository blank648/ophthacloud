package ro.unibuc.fmi.ophthacloud.common.api;

import java.util.List;

public record PagedResponse<T>(
        List<T> items,
        int page,
        int size,
        long total
) {
    public PagedResponse {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
