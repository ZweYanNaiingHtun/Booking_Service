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

    @Override
    @Transactional
    public void deleteService(Long id) {
        // ၁။ DB ထဲတွင် ရှိမရှိ စစ်ဆေးခြင်း
        BusinessService service = this.serviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found with id: " + id));

        // 🌟 Constraint Check: ဤ Service သည် Package တစ်ခုခု၏ bundledServices ထဲတွင် ပါဝင်နေပါက Soft Delete ခွင့်မပြုပါ
        boolean isBundled = this.serviceRepository.isServiceBundledInAnyPackage(id);
        if (isBundled) {
            throw new BadRequestException("Cannot delete service '" + service.getName() + "' because it is currently included in one or more packages.");
        }

        // ၂။ Service အား Soft Delete ပြုလုပ်ခြင်း
        service.setEnabled(false);
        this.serviceRepository.save(service);

        // 🌟 ၃။ [AUTO-DISABLE CATEGORY] Category အောက်တွင် Active ဖြစ်သော Service ကျန်သေးလား စစ်ဆေးခြင်း
        Category category = service.getCategory();
        if (category != null) {
            boolean hasActiveServicesLeft = this.serviceRepository.existsByCategoryIdAndIsEnabledTrue(category.getId());

            // အကယ်၍ Active Service တစ်ခုမျှ မကျန်တော့ပါက Parent Category ကိုပါ enabled = false ပြောင်းပါမည်
            if (!hasActiveServicesLeft) {
                category.setEnabled(false);
                this.categoryRepository.save(category);
            }
        }
    }

    @Override
    @Transactional
    public void restoreService(Long id) {
        // ၁။ DB ထဲတွင် ရှိမရှိ စစ်ဆေးခြင်း
        BusinessService service = this.serviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found with id: " + id));

        // ၂။ Active ဖြစ်ပြီးသား လား စစ်ဆေးခြင်း
        if (service.isEnabled()) {
            throw new BadRequestException("Service is already active and does not need to be restored.");
        }

        // ၃။ Service အား ပြန်လည် Active (enabled = true) ပြုလုပ်ခြင်း
        service.setEnabled(true);
        this.serviceRepository.save(service);

        // 🌟 ၄။ [AUTO-ENABLE CATEGORY] Parent Category သည် disabled ဖြစ်နေပါက enabled = true သို့ အလိုအလျောက် ပြန်ဖွင့်ပေးခြင်း
        Category category = service.getCategory();
        if (category != null && !category.isEnabled()) {
            category.setEnabled(true);
            this.categoryRepository.save(category);
        }
    }

    private ServiceResponse mapToResponse(BusinessService service) {
        List<String> serviceNames = service.getBundledServices().stream()
                .map(BusinessService::getName)
                .toList();
        return ServiceResponse.builder()
                .id(service.getId())
                .name(service.getName())
                .description(service.getDescription())
                .price(service.getPrice())
                .categoryId(service.getCategory().getId())
                .categoryName(service.getCategory().getName())
                .durationInMinutes(service.getDurationInMinutes())
                .isPackage(service.is_package())
                .includedServices(serviceNames)
                .isEnabled(service.isEnabled())
                .build();
    }
}
