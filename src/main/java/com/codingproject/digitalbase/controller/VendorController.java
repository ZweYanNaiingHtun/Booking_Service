//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.codingproject.digitalbase.controller;

import com.codingproject.digitalbase.dtos.VendorRequest;
import com.codingproject.digitalbase.dtos.VendorResponse;
import com.codingproject.digitalbase.service.VendorService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/vendor"})
@RequiredArgsConstructor
public class VendorController {
    private final VendorService vendorService;

    @PostMapping(
            consumes = {"multipart/form-data"}
    )
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<VendorResponse> addVendor(@ModelAttribute VendorRequest request) {
        return ResponseEntity.ok(this.vendorService.createVendor(request));
    }

    @GetMapping
    public ResponseEntity<List<VendorResponse>> getAllVendors() {
        return ResponseEntity.ok(this.vendorService.getAllVendors());
    }

    @GetMapping({"/{id}"})
    public ResponseEntity<VendorResponse> getVendorById(@PathVariable Long id) {
        return ResponseEntity.ok(this.vendorService.getVendorDetails(id));
    }

    @PutMapping(
            value = {"/{id}"},
            consumes = {"multipart/form-data"}
    )
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<VendorResponse> updateVendor(@PathVariable Long id, @ModelAttribute VendorRequest request) {
        return ResponseEntity.ok(this.vendorService.updateVendorDetails(id, request));
    }

    @DeleteMapping({"/{id}"})
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deleteVendor(@PathVariable Long id) {
        this.vendorService.deleteVendor(id);
        return ResponseEntity.noContent().build();
    }
}
