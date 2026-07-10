package com.codingproject.digitalbase.service;

import com.codingproject.digitalbase.dtos.VendorRequest;
import com.codingproject.digitalbase.dtos.VendorResponse;
import com.codingproject.digitalbase.model.Vendor;
import com.codingproject.digitalbase.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VendorServiceImpl implements VendorService {

    private final VendorRepository vendorRepository;

    // ပုံများ သိမ်းဆည်းမည့် Folder လမ်းကြောင်း (Project Root အောက်က uploads/ ပုံစံမျိုး)
    private final String UPLOAD_DIR = "uploads/";

    @Override
    @Transactional
    public VendorResponse createVendor(VendorRequest request) {
        String finalBannerImageName = null;

        // 🌟 Multipart File အား သိမ်းဆည်းခြင်း Logic
        if (request.getBannerFile() != null && !request.getBannerFile().isEmpty()) {
            try {
                File uploadFolder = new File(UPLOAD_DIR);
                if (!uploadFolder.exists()) {
                    uploadFolder.mkdirs(); // Folder မရှိပါက ဆောက်ပေးမည်
                }

                // နာမည်ချင်း မတူအောင် Timestamp ကပ်ပေးမည်
                finalBannerImageName = System.currentTimeMillis() + "_" + request.getBannerFile().getOriginalFilename();
                Path filePath = Paths.get(UPLOAD_DIR, finalBannerImageName);
                Files.copy(request.getBannerFile().getInputStream(), filePath);

            } catch (IOException e) {
                throw new RuntimeException("Failed to upload banner image: " + e.getMessage());
            }
        }

        // 🌟 eventActive အား Default True ပေးခြင်း Logic
        boolean isActive = (request.getEventActive() != null) ? request.getEventActive() : true;

        Vendor vendor = Vendor.builder()
                .name(request.getName())
                .logo(request.getLogo())
                .bannerTitle(request.getBannerTitle())
                .bannerDescription(request.getBannerDescription())
                .bannerImage(finalBannerImageName) // 🌟 သိမ်းဆည်းရရှိသော File Name ထည့်မည်
                .eventStartDate(request.getEventStartDate())
                .eventEndDate(request.getEventEndDate())
                .eventActive(isActive) // 🌟
                .build();

        Vendor saved = vendorRepository.save(vendor);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VendorResponse> getAllVendors() {
        return vendorRepository.findAll().stream().map(this::mapToResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public VendorResponse getVendorDetails(Long id) {
        Vendor vendor = vendorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vendor not found with id: " + id));
        return mapToResponse(vendor);
    }

    @Override
    @Transactional
    public VendorResponse updateVendorDetails(Long id, VendorRequest request) {
        Vendor vendor = vendorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vendor not found with id: " + id));

        vendor.setName(request.getName());
        if (request.getLogo() != null) vendor.setLogo(request.getLogo());
        vendor.setBannerTitle(request.getBannerTitle());
        vendor.setBannerDescription(request.getBannerDescription());

        // Update လုပ်စဉ် ပုံအသစ်ပါလာပါက ထပ်မံသိမ်းဆည်းမည်
        if (request.getBannerFile() != null && !request.getBannerFile().isEmpty()) {
            try {
                String finalBannerImageName = System.currentTimeMillis() + "_" + request.getBannerFile().getOriginalFilename();
                Files.copy(request.getBannerFile().getInputStream(), Paths.get(UPLOAD_DIR, finalBannerImageName));
                vendor.setBannerImage(finalBannerImageName);
            } catch (IOException e) {
                throw new RuntimeException("Failed to update banner image: " + e.getMessage());
            }
        }

        vendor.setEventStartDate(request.getEventStartDate());
        vendor.setEventEndDate(request.getEventEndDate());
        if (request.getEventActive() != null) vendor.setEventActive(request.getEventActive());

        Vendor updated = vendorRepository.save(vendor);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void deleteVendor(Long id) {
        if (!vendorRepository.existsById(id)) {
            throw new RuntimeException("Vendor not found with id: " + id);
        }
        vendorRepository.deleteById(id);
    }

    private VendorResponse mapToResponse(Vendor v) {

        String bannerUrl = (v.getBannerImage() != null && !v.getBannerImage().isEmpty())
                ? "/uploads/" + v.getBannerImage()
                : null;
        return VendorResponse.builder()
                .id(v.getId())
                .name(v.getName())
                .logo(v.getLogo())
                .bannerTitle(v.getBannerTitle())
                .bannerDescription(v.getBannerDescription())
                .bannerImage(bannerUrl)
                .eventStartDate(v.getEventStartDate())
                .eventEndDate(v.getEventEndDate())
                .eventActive(v.isEventActive()) // Entity ဘက်မှ Getter ဖြစ်ပါသည်
                .build();
    }
}