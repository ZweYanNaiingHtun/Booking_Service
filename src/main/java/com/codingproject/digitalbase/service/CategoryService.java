//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.codingproject.digitalbase.service;

import com.codingproject.digitalbase.dtos.CategoryRequest;
import com.codingproject.digitalbase.dtos.CategoryWithServicesResponse;
import com.codingproject.digitalbase.model.Category;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

public interface CategoryService {
    @Transactional(
            readOnly = true
    )
    CategoryWithServicesResponse getCategoryById(Long id);

    List<Category> getAllCategories();

    Category createCategory(CategoryRequest request);

    void deleteCategory(Long id);

    Category updateCategory(Long id, CategoryRequest request);
}
