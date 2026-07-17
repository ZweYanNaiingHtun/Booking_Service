//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.codingproject.digitalbase.controller;

import com.codingproject.digitalbase.dtos.ServiceRequest;
import com.codingproject.digitalbase.dtos.ServiceResponse;
import com.codingproject.digitalbase.service.BusinessServiceService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

import lombok.Generated;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping({"/api/services"})
public class BusinessServiceController {
    private final BusinessServiceService businessService;

    @GetMapping
    public ResponseEntity<List<ServiceResponse>> getAllServices() {
        return ResponseEntity.ok(this.businessService.getAllServices());
    }

    @GetMapping({"/{id}"})
    public ResponseEntity<ServiceResponse> getServiceById(@PathVariable Long id) {
        return ResponseEntity.ok(this.businessService.getServiceById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ServiceResponse> createService(@RequestBody @Valid ServiceRequest request) {
        return ResponseEntity.ok(this.businessService.createService(request));
    }

    @PutMapping({"/{id}"})
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ServiceResponse> updateService(@PathVariable Long id, @RequestBody @Valid ServiceRequest request) {
        return ResponseEntity.ok(this.businessService.updateService(id, request));
    }

    @DeleteMapping({"/{id}"})
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, String>> deleteService(@PathVariable Long id) {
        this.businessService.deleteService(id);

        // 🌟 String အစား Frontend အတွက် JSON Object (Key-Value) ပြောင်းလဲပေးခြင်း
        return ResponseEntity.ok(Map.of("message", "Service deleted successfully"));
    }

    @PutMapping({"/{id}/restore"})
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, String>> restoreService(@PathVariable Long id) {
        this.businessService.restoreService(id);

        // 🌟 String အစား Frontend အတွက် JSON Object (Key-Value) ပြောင်းလဲပေးခြင်း
        return ResponseEntity.ok(Map.of("message", "Service has been restored successfully and is now active."));
    }
}
