//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.codingproject.digitalbase.service;

import com.codingproject.digitalbase.dtos.ServiceRequest;
import com.codingproject.digitalbase.dtos.ServiceResponse;
import com.codingproject.digitalbase.exception.BadRequestException;
import com.codingproject.digitalbase.exception.ResourceNotFoundException;
import com.codingproject.digitalbase.model.BusinessService;
import com.codingproject.digitalbase.model.Category;
import com.codingproject.digitalbase.repository.BusinessServiceRepository;
import com.codingproject.digitalbase.repository.CategoryRepository;
import java.util.List;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BusinessServiceServiceImpl implements BusinessServiceService {
    private final BusinessServiceRepository serviceRepository;
    private final CategoryRepository categoryRepository;

    public List<ServiceResponse> getAllServices() {
        return this.serviceRepository.findAll().stream().map(this::mapToResponse).toList();
    }

    @Transactional(
            readOnly = true
    )
    public ServiceResponse getServiceById(Long id) {
        BusinessService service = this.serviceRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Service not found"));
        return this.mapToResponse(service);
    }

    @Transactional
    public ServiceResponse createService(ServiceRequest request) {
        if (this.serviceRepository.existsByName(request.getName())) {
            throw new BadRequestException("Service name already exists");
        } else {
            Category category = this.categoryRepository.findById(request.getCategoryId()).orElseThrow(() -> new ResourceNotFoundException("Category not found"));
            BusinessService service = new BusinessService();
            service.setName(request.getName());
            service.setDescription(request.getDescription());
            service.setPrice(request.getPrice());
            service.setCategory(category);
            service.setDurationInMinutes(request.getDurationInMinutes());
            service.set_package(request.isPackage());
            return this.mapToResponse(this.serviceRepository.save(service));
        }
    }

    @Transactional
    public ServiceResponse updateService(Long id, ServiceRequest request) {
        BusinessService service = this.serviceRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Service not found"));
        if (!service.getName().equals(request.getName()) && this.serviceRepository.existsByName(request.getName())) {
            throw new BadRequestException("Service name already exists");
        } else {
            Category category = this.categoryRepository.findById(request.getCategoryId()).orElseThrow(() -> new ResourceNotFoundException("Category not found"));
            service.setName(request.getName());
            service.setDescription(request.getDescription());
            service.setPrice(request.getPrice());
            service.setCategory(category);
            service.setDurationInMinutes(request.getDurationInMinutes());
            service.set_package(request.isPackage());
            return this.mapToResponse(this.serviceRepository.save(service));
        }
    }

    @Transactional
    public void deleteService(Long id) {
        BusinessService service = this.serviceRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Service not found"));
        service.setEnabled(false);
        this.serviceRepository.save(service);
    }

    @Transactional
    public void restoreService(Long id) {
        BusinessService service = this.serviceRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Service not found with id: " + id));
        if (service.isEnabled()) {
            throw new BadRequestException("Service is already active and does not need to be restored.");
        } else {
            service.setEnabled(true);
            this.serviceRepository.save(service);
        }
    }

    private ServiceResponse mapToResponse(BusinessService service) {
        return ServiceResponse.builder().id(service.getId()).name(service.getName()).description(service.getDescription()).price(service.getPrice()).categoryId(service.getCategory().getId()).categoryName(service.getCategory().getName()).durationInMinutes(service.getDurationInMinutes()).isPackage(service.is_package()).isEnabled(service.isEnabled()).build();
    }
}
