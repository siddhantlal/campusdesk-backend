package dev.campusdesk.common;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

public final class Paging {
    private Paging() {}
    public static PageRequest request(int page, int size, String field, String direction) {
        if (page < 0 || page > 100_000 || size < 1 || size > 100)
            throw new ApiException(400, "INVALID_PAGE", "Page must be 0..100000 and size 1..100");
        if (!field.equals("createdAt") && !field.equals("updatedAt"))
            throw new ApiException(400, "INVALID_SORT", "Sort must be createdAt or updatedAt");
        if (!direction.equalsIgnoreCase("asc") && !direction.equalsIgnoreCase("desc"))
            throw new ApiException(400, "INVALID_SORT", "Direction must be asc or desc");
        Sort.Direction dir = Sort.Direction.fromString(direction);
        return PageRequest.of(page, size, Sort.by(dir, field).and(Sort.by(dir, "id")));
    }
}
