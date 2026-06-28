//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.codingproject.digitalbase.service;

import com.codingproject.digitalbase.dtos.CategoryRequest;
import com.codingproject.digitalbase.dtos.CategoryWithServicesResponse;
import com.codingproject.digitalbase.dtos.ServiceResponse;
import com.codingproject.digitalbase.exception.BadRequestException;
import com.codingproject.digitalbase.exception.ResourceNotFoundException;
import com.codingproject.digitalbase.model.Category;
import com.codingproject.digitalbase.repository.CategoryRepository;
import java.util.List;
import lombok.Generated;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;

    @Transactional(
            readOnly = true
    )
    public CategoryWithServicesResponse getCategoryById(Long id) {
        Category category = (Category)this.categoryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        List<ServiceResponse> serviceResponses = category.getServices().stream().map((service) -> ServiceResponse.builder().id(service.getId()).name(service.getName()).description(service.getDescription()).price(service.getPrice()).isPackage(service.is_package()).isEnabled(service.isEnabled()).categoryId(category.getId()).categoryName(category.getName()).durationInMinutes(service.getDurationInMinutes()).build()).toList();
        return new CategoryWithServicesResponse(category.getId(), category.getName(), serviceResponses);
    }

    public List<Category> getAllCategories() {
        return this.categoryRepository.findAll();
    }

    @Transactional
    public Category createCategory(CategoryRequest request) {
        if (this.categoryRepository.existsByName(request.getName())) {
            throw new BadRequestException("Category name already exists");
        } else {
            Category category = new Category();
            category.setName(request.getName());
            return (Category)this.categoryRepository.save(category);
        }
    }

    @Transactional
    public Category updateCategory(Long id, CategoryRequest request) {
        Category category = (Category)this.categoryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        if (!category.getName().equals(request.getName()) && this.categoryRepository.existsByName(request.getName())) {
            throw new BadRequestException("Category name already exists");
        } else {
            category.setName(request.getName());
            return (Category)this.categoryRepository.save(category);
        }
    }

    @Transactional
    public void deleteCategory(Long id) {
        if (!this.categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Category not found");
        } else {
            this.categoryRepository.deleteById(id);
        }
    }

    @Generated
    public CategoryServiceImpl(final CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }
}
