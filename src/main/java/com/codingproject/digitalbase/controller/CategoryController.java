//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.codingproject.digitalbase.controller;

import com.codingproject.digitalbase.dtos.CategoryRequest;
import com.codingproject.digitalbase.dtos.CategoryResponse;
import com.codingproject.digitalbase.dtos.CategoryWithServicesResponse;
import com.codingproject.digitalbase.model.Category;
import com.codingproject.digitalbase.service.CategoryService;
import java.util.List;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping({"/api/categories"})
@CrossOrigin(origins = {"*"})
public class CategoryController {
    private final CategoryService categoryService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'STAFF', 'CUSTOMER')")
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        List<CategoryResponse> responses = this.categoryService.getAllCategories().stream().map((category) -> new CategoryResponse(category.getId(), category.getName())).toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping({"/{id}"})
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'STAFF', 'CUSTOMER')")
    public ResponseEntity<CategoryWithServicesResponse> getCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(this.categoryService.getCategoryById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN')")
    public ResponseEntity<CategoryResponse> createCategory(@RequestBody CategoryRequest request) {
        Category category = this.categoryService.createCategory(request);
        CategoryResponse response = new CategoryResponse(category.getId(), category.getName());
        return new ResponseEntity(response, HttpStatus.CREATED);
    }

    @PutMapping({"/{id}"})
    @PreAuthorize("hasAnyRole('SUPER_ADMIN')")
    public ResponseEntity<CategoryResponse> updateCategory(@PathVariable Long id, @RequestBody CategoryRequest request) {
        Category category = this.categoryService.updateCategory(id, request);
        CategoryResponse response = new CategoryResponse(category.getId(), category.getName());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping({"/{id}"})
    @PreAuthorize("hasAnyRole('SUPER_ADMIN')")
    public ResponseEntity<String> deleteCategory(@PathVariable Long id) {
        this.categoryService.deleteCategory(id);
        return ResponseEntity.ok("Category deleted successfully");
    }
}
