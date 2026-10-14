package dev.campusdesk.categories;

import jakarta.validation.constraints.*;

public final class CategoryDtos {
    private CategoryDtos() {}
    public record CreateCategory(@NotBlank @Size(max=80) String name,@NotBlank @Size(max=500) String description) {}
    public record UpdateCategory(@NotBlank @Size(max=80) String name,@NotBlank @Size(max=500) String description,
                                 @NotNull Boolean active,@NotNull @PositiveOrZero Long version) {}
    public record CategoryResponse(Long id,String name,String description,boolean active,long version) {
        public static CategoryResponse of(Category c) { return new CategoryResponse(c.getId(),c.getName(),c.getDescription(),c.isActive(),c.getVersion()); }
    }
}
