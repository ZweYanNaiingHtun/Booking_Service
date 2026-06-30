//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.codingproject.digitalbase.controller;

import com.codingproject.digitalbase.dtos.StaffCreateRequest;
import com.codingproject.digitalbase.dtos.StaffResponse;
import com.codingproject.digitalbase.dtos.StaffUpdateRequest;
import com.codingproject.digitalbase.service.StaffManagementService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping({"/api/admin/staffs"})
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class StaffManagementController {
    private final StaffManagementService staffService;

    @PostMapping
    public ResponseEntity<StaffResponse> createStaff(@RequestBody @Valid StaffCreateRequest request) {
        StaffResponse response = this.staffService.createStaffUser(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping({"/{staffId}/services"})
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<StaffResponse> linkServicesToStaff(@PathVariable("staffId") Long staffId, @RequestBody List<Long> serviceIds) {
        StaffResponse response = this.staffService.linkStaffWithServices(staffId, serviceIds);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<StaffResponse>> getAllStaffs() {
        return ResponseEntity.ok(this.staffService.getAllStaffs());
    }

    @PutMapping(
            value = {"/{id}"},
            consumes = {"multipart/form-data"}
    )
    public ResponseEntity<StaffResponse> updateStaff(@PathVariable Long id, @ModelAttribute @Valid StaffUpdateRequest request) {
        return ResponseEntity.ok(this.staffService.updateStaffUser(id, request));
    }

    @PutMapping({"/{id}/status"})
    public ResponseEntity<StaffResponse> toggleStaffStatus(@PathVariable Long id, @RequestParam boolean enable) {
        return ResponseEntity.ok(this.staffService.toggleStaffStatus(id, enable));
    }
}
