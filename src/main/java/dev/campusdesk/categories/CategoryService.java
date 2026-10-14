package dev.campusdesk.categories;

import dev.campusdesk.common.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static dev.campusdesk.categories.CategoryDtos.*;

@Service
public class CategoryService {
    private final CategoryRepository categories;
    public CategoryService(CategoryRepository categories) { this.categories=categories; }
    @Transactional(readOnly=true)
    public PageResponse<CategoryResponse> list(boolean includeInactive,int page,int size) {
        Paging.request(page,size,"createdAt","asc");
        var paging=PageRequest.of(page,size,Sort.by("name").and(Sort.by("id")));
        return PageResponse.of((includeInactive ? categories.findAll(paging) : categories.findByActiveTrue(paging)).map(CategoryResponse::of));
    }
    @Transactional
    public CategoryResponse create(CreateCategory request) {
        if (categories.existsByNameIgnoreCase(request.name().trim())) throw new ApiException(409,"CATEGORY_EXISTS","Category name already exists");
        return CategoryResponse.of(categories.saveAndFlush(new Category(request.name(),request.description())));
    }
    @Transactional
    public CategoryResponse update(long id,UpdateCategory request) {
        Category category=categories.findById(id).orElseThrow(() -> ApiException.missing("Category"));
        if (category.getVersion()!=request.version()) throw ApiException.conflict();
        if (categories.existsByNameIgnoreCaseAndIdNot(request.name().trim(),id)) throw new ApiException(409,"CATEGORY_EXISTS","Category name already exists");
        category.update(request.name(),request.description(),request.active()); categories.flush();
        return CategoryResponse.of(category);
    }
}
