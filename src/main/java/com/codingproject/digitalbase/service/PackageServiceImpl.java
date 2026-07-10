package com.codingproject.digitalbase.service;

import com.codingproject.digitalbase.dtos.PackageRequest;
import com.codingproject.digitalbase.dtos.PackageResponse;
import com.codingproject.digitalbase.exception.BadRequestException;
import com.codingproject.digitalbase.exception.ResourceNotFoundException;
import com.codingproject.digitalbase.model.BusinessService;
import com.codingproject.digitalbase.model.Category;
import com.codingproject.digitalbase.repository.BusinessServiceRepository;
import com.codingproject.digitalbase.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PackageServiceImpl implements PackageService {

    private final BusinessServiceRepository serviceRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public PackageResponse createPackage(PackageRequest request) {
        if (serviceRepository.existsByName(request.getName())) {
            throw new BadRequestException("Package name already exists");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (!category.getName().toLowerCase().contains("package")) {
            throw new BadRequestException("Package creation denied! Packages can only be added to categories designated for packages (Category name must contain 'Package').");
        }

        if (request.getServiceIds() == null || request.getServiceIds().size() < 2 || request.getServiceIds().size() > 3) {
            throw new BadRequestException("Package creation denied! A package must contain exactly 2 or 3 services. (You selected: "
                    + (request.getServiceIds() != null ? request.getServiceIds().size() : 0) + ")");
        }

        List<BusinessService> selectedServices = serviceRepository.findAllById(request.getServiceIds());
        if (selectedServices.size() != request.getServiceIds().size()) {
            throw new BadRequestException("One or more selected service IDs are invalid.");
        }

        // 🎯 🌟 [NEW RESTRICTION] ရွေးချယ်ထားသော ဝန်ဆောင်မှုများထဲတွင် အခြား Package ပါဝင်နေပါက တားဆီးခြင်း (Pure Services Only)
        boolean hasNestedPackage = selectedServices.stream().anyMatch(BusinessService::is_package);
        if (hasNestedPackage) {
            throw new BadRequestException("Package creation denied! A package can only contain pure standard services, not other packages.");
        }

        int totalDuration = selectedServices.stream()
                .mapToInt(BusinessService::getDurationInMinutes)
                .sum();

        BusinessService businessPackage = new BusinessService();
        businessPackage.setName(request.getName());
        businessPackage.setPrice(request.getPrice());
        businessPackage.setCategory(category);
        businessPackage.setDurationInMinutes(totalDuration);
        businessPackage.set_package(true);
        businessPackage.setEnabled(true);
        businessPackage.setBundledServices(selectedServices);

        return mapToPackageResponse(serviceRepository.save(businessPackage));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PackageResponse> getAllPackages() {
        return serviceRepository.findAll().stream()
                .filter(BusinessService::is_package)
                .map(this::mapToPackageResponse)
                .toList();
    }

    // 🎯 🌟 [ADDED] ID ဖြင့် Package အား ဆွဲထုတ်ခြင်း Logic
    @Override
    @Transactional(readOnly = true)
    public PackageResponse getPackageById(Long id) {
        BusinessService service = serviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found with id: " + id));

        // တကယ်လို့ ရိုးရိုး service ဖြစ်နေရင် တားဆီးရန်
        if (!service.is_package()) {
            throw new BadRequestException("The requested ID is a standard service, not a package.");
        }

        return mapToPackageResponse(service);
    }

    // 🎯 🌟 [ADDED] Package အား ပြန်လည်ပြင်ဆင်ခြင်း Logic
    @Override
    @Transactional
    public PackageResponse updatePackage(Long id, PackageRequest request) {
        BusinessService businessPackage = serviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found with id: " + id));

        if (!businessPackage.is_package()) {
            throw new BadRequestException("The requested ID is a standard service, not a package.");
        }

        if (!businessPackage.getName().equals(request.getName()) && serviceRepository.existsByName(request.getName())) {
            throw new BadRequestException("Package name already exists");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (!category.getName().toLowerCase().contains("package")) {
            throw new BadRequestException("Package update denied! Packages can only be added to categories designated for packages (Category name must contain 'Package').");
        }

        if (request.getServiceIds() == null || request.getServiceIds().size() < 2 || request.getServiceIds().size() > 3) {
            throw new BadRequestException("Package update denied! A package must contain exactly 2 or 3 services. (You selected: "
                    + (request.getServiceIds() != null ? request.getServiceIds().size() : 0) + ")");
        }

        List<BusinessService> selectedServices = serviceRepository.findAllById(request.getServiceIds());
        if (selectedServices.size() != request.getServiceIds().size()) {
            throw new BadRequestException("One or more selected service IDs are invalid.");
        }

        // 🎯 🌟 [NEW RESTRICTION] ပြင်ဆင်သည့်အခါတွင်လည်း အခြား Package များ ရောနှောမလာစေရန် တားဆီးခြင်း (Pure Services Only)
        boolean hasNestedPackage = selectedServices.stream().anyMatch(BusinessService::is_package);
        if (hasNestedPackage) {
            throw new BadRequestException("Package update denied! A package can only contain pure standard services, not other packages.");
        }

        int newTotalDuration = selectedServices.stream()
                .mapToInt(BusinessService::getDurationInMinutes)
                .sum();

        businessPackage.setName(request.getName());
        businessPackage.setPrice(request.getPrice());
        businessPackage.setCategory(category);
        businessPackage.setDurationInMinutes(newTotalDuration);
        businessPackage.setBundledServices(selectedServices);

        return mapToPackageResponse(serviceRepository.save(businessPackage));
    }

    private PackageResponse mapToPackageResponse(BusinessService service) {
        List<String> serviceNames = service.getBundledServices().stream()
                .map(BusinessService::getName)
                .toList();

        return PackageResponse.builder()
                .id(service.getId())
                .name(service.getName())
                .price(service.getPrice())
                .durationInMinutes(service.getDurationInMinutes())
                .isPackage(service.is_package())
                .isEnabled(service.isEnabled())
                .categoryId(service.getCategory().getId())
                .categoryName(service.getCategory().getName())
                .includedServices(serviceNames)
                .build();
    }
}