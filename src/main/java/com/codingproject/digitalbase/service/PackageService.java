package com.codingproject.digitalbase.service;

import com.codingproject.digitalbase.dtos.PackageRequest;
import com.codingproject.digitalbase.dtos.PackageResponse;
import java.util.List;

public interface PackageService {
    PackageResponse createPackage(PackageRequest request);
    List<PackageResponse> getAllPackages();
    PackageResponse getPackageById(Long id);
    PackageResponse updatePackage(Long id, PackageRequest request);

    // 🌟 Soft Delete & Restore Methods
    void deletePackage(Long id);
    void restorePackage(Long id);
}