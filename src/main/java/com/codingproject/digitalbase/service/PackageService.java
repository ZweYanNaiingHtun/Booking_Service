package com.codingproject.digitalbase.service;

import com.codingproject.digitalbase.dtos.PackageRequest;
import com.codingproject.digitalbase.dtos.PackageResponse;

import java.util.List;

public interface PackageService {

    PackageResponse createPackage(PackageRequest request);

    List<PackageResponse> getAllPackages();

    // 🎯 🌟 [ADDED] ID အလိုက် Package တစ်ခုတည်းကို ရှာဖွေသည့် မက်သတ်
    PackageResponse getPackageById(Long id);

    // 🎯 🌟 [ADDED] Package အား ပြန်လည်ပြင်ဆင်သည့် မက်သတ်
    PackageResponse updatePackage(Long id, PackageRequest request);
}
