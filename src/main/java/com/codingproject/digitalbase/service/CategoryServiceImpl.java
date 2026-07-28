package com.codingproject.digitalbase.service;

import com.codingproject.digitalbase.dtos.CategoryRequest;
import com.codingproject.digitalbase.dtos.CategoryResponse;
import com.codingproject.digitalbase.dtos.CategoryWithServicesResponse;
import com.codingproject.digitalbase.dtos.ServiceResponse;
import com.codingproject.digitalbase.exception.BadRequestException;
import com.codingproject.digitalbase.exception.ResourceNotFoundException;
import com.codingproject.digitalbase.model.Category;
import com.codingproject.digitalbase.repository.BusinessServiceRepository;
import com.codingproject.digitalbase.repository.CategoryRepository;
import java.util.List;
import java.util.Set;

import lombok.Generated;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final BusinessServiceRepository serviceRepository;

    @Transactional(readOnly = true)
    public CategoryWithServicesResponse getCategoryById(Long id) {
        Category category = this.categoryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        List<ServiceResponse> serviceResponses = category.getServices().stream().map((service) -> ServiceResponse.builder().id(service.getId()).name(service.getName()).description(service.getDescription()).price(service.getPrice()).isPackage(service.is_package()).isEnabled(service.isEnabled()).categoryId(category.getId()).categoryName(category.getName()).durationInMinutes(service.getDurationInMinutes()).build()).toList();
        return new CategoryWithServicesResponse(category.getId(), category.getName(), serviceResponses);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        // 1. Category အားလုံးကို Fetch လုပ်ခြင်း
        List<Category> categories = categoryRepository.findAll();

        // 🌟 2. Package ထဲတွင် ပါဝင်နေသော Category ID များအားလုံးကို တစ်ခါတည်း ဆွဲထုတ်ခြင်း (N+1 Query Error မှ ကာကွယ်ရန်)
        Set<Long> categoryIdsInPackages = serviceRepository.findCategoryIdsInPackages();

        // 3. DTO သို့ Mapping ပြုလုပ်ခြင်း
        return categories.stream().map(category -> {
            // categoryIdsInPackages Set ထဲတွင် ဤ Category ID ပါနေပါက true ဖြစ်မည်
            boolean isBundledInPackage = categoryIdsInPackages.contains(category.getId());

            return CategoryResponse.builder()
                    .id(category.getId())
                    .name(category.getName())
                    .enabled(category.isEnabled())
                    .inPackage(isBundledInPackage) // 🌟 inPackage value ထည့်သွင်းခြင်း
                    .build();
        }).toList();
    }

    @Transactional
    public Category createCategory(CategoryRequest request) {
        if (this.categoryRepository.existsByName(request.getName())) {
            throw new BadRequestException("Category name already exists");
        } else {
            Category category = new Category();
            category.setName(request.getName());
            return this.categoryRepository.save(category);
        }
    }

    @Transactional
    public Category updateCategory(Long id, CategoryRequest request) {
        Category category = this.categoryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        if (!category.getName().equals(request.getName()) && this.categoryRepository.existsByName(request.getName())) {
            throw new BadRequestException("Category name already exists");
        } else {
            category.setName(request.getName());
            return this.categoryRepository.save(category);
        }
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        // ၁။ DB ထဲတွင် ရှိမရှိ စစ်ဆေးခြင်း
        Category category = this.categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        // 🌟 Constraint Check: ဤ Category အောက်ရှိ Service တစ်ခုခုသည် Package ထဲတွင် ပါဝင်နေပါက Parent Category အား Soft Delete ခွင့်မပြုပါ
        boolean hasServiceInPackage = this.serviceRepository.isCategoryServiceBundledInAnyPackage(id);
        if (hasServiceInPackage) {
            throw new BadRequestException("Cannot delete category '" + category.getName() + "' because one or more services under this category are included in packages.");
        }

        // Soft Delete ပြုလုပ်ခြင်း
        category.setEnabled(false);
        this.categoryRepository.save(category);
    }

    @Override
    @Transactional
    public void restoreCategory(Long id) {
        // ၁။ DB ထဲတွင် ရှိမရှိ အရင်စစ်မည်
        Category category = this.categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        // ၂။ ပြန်လည်အသုံးပြုနိုင်ရန် enabled = true ပြောင်းပေးမည် (Restore)
        category.setEnabled(true);
        this.categoryRepository.save(category);
    }
}
