package com.codingproject.digitalbase.controller;

import com.codingproject.digitalbase.dtos.PackageRequest;
import com.codingproject.digitalbase.dtos.PackageResponse;
import com.codingproject.digitalbase.service.PackageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/packages")
@RequiredArgsConstructor
public class PackageController {

    private final PackageService packageService;

    /**
     * 🎯 [POST] Package အသစ်ဆောက်ရန်
     */
    @PostMapping
    public ResponseEntity<PackageResponse> createPackage(@RequestBody PackageRequest request) {
        PackageResponse response = packageService.createPackage(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * 🎯 [GET] Package အားလုံးကို စာရင်းယူရန်
     */
    @GetMapping
    public ResponseEntity<List<PackageResponse>> getAllPackages() {
        List<PackageResponse> response = packageService.getAllPackages();
        return ResponseEntity.ok(response);
    }

    /**
     * 🎯 [GET] ID အလိုက် Package တစ်ခုချင်းစီ၏ အသေးစိတ်ကို ယူရန် (ဥပမာ Edit Form ထဲ ဒေတာကြိုပြရန်)
     */
    @GetMapping("/{id}")
    public ResponseEntity<PackageResponse> getPackageById(@PathVariable Long id) {
        PackageResponse response = packageService.getPackageById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PackageResponse> updatePackage(
            @PathVariable Long id,
            @RequestBody PackageRequest request) {
        PackageResponse response = packageService.updatePackage(id, request);
        return ResponseEntity.ok(response);
    }

    // 🌟 1. Package အား ပိတ်ပစ်မည့် Soft Delete Endpoint
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, String>> deletePackage(@PathVariable Long id) {
        this.packageService.deletePackage(id);
        return ResponseEntity.ok(Map.of("message", "Package deleted successfully"));
    }

    // 🌟 2. Package အား ပြန်လည်ဖွင့်ပေးမည့် Restore Endpoint
    @PutMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, String>> restorePackage(@PathVariable Long id) {
        this.packageService.restorePackage(id);
        return ResponseEntity.ok(Map.of("message", "Package restored successfully"));
    }
}