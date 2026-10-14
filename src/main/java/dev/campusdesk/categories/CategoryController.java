package dev.campusdesk.categories;

import jakarta.validation.Valid;
import dev.campusdesk.auth.CurrentUser;
import dev.campusdesk.users.Role;
import dev.campusdesk.common.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import static dev.campusdesk.categories.CategoryDtos.*;

@RestController @RequestMapping("/api/v1/categories")
public class CategoryController {
    private final CategoryService categories;
    public CategoryController(CategoryService categories) { this.categories=categories; }
    @GetMapping
    public PageResponse<CategoryResponse> list(@AuthenticationPrincipal CurrentUser caller,
            @RequestParam(defaultValue="false") boolean includeInactive,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        if (includeInactive && caller.role()!=Role.ADMIN) throw new ApiException(403,"FORBIDDEN","Only administrators can list inactive categories");
        return categories.list(includeInactive,page,size);
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('ADMIN')")
    public CategoryResponse create(@Valid @RequestBody CreateCategory request) { return categories.create(request); }
    @PatchMapping("/{id}") @PreAuthorize("hasRole('ADMIN')")
    public CategoryResponse update(@PathVariable long id,@Valid @RequestBody UpdateCategory request) { return categories.update(id,request); }
}
