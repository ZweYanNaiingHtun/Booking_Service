//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.codingproject.digitalbase.controller;

import com.codingproject.digitalbase.dtos.*;
import com.codingproject.digitalbase.service.StaffManagementService;
import jakarta.validation.Valid;

import java.time.Instant;
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

//    @PutMapping({"/{staffId}/services"})
//    @PreAuthorize("hasRole('SUPER_ADMIN')")
//    public ResponseEntity<StaffResponse> linkServicesToStaff(@PathVariable("staffId") Long staffId, @RequestBody List<Long> serviceIds) {
//        StaffResponse response = this.staffService.linkStaffWithServices(staffId, serviceIds);
//        return ResponseEntity.ok(response);
//    }

    // 🎯 ၁။ နေ့အလိုက် ဝန်ထမ်းအခြေအနေ (Sidebar) - Instant သုံးထားပါသည်
    @GetMapping("/daily-status")
    public ResponseEntity<DailyStaffStatusResponse> getDailyStatus(@RequestParam Instant date) {
        return ResponseEntity.ok(staffService.getDailyStaffStatus(date));
    }

    // 🎯 ၂။ Calendar ရက်အလိုက် Events များနှင့် Staff Filter (Start/End Range ကို Instant ဖြင့် ယူပါသည်)
    @GetMapping("/calendar")
    public ResponseEntity<List<CalendarMonthResponse>> getCalendarOverview(
            @RequestParam Instant startDate,
            @RequestParam Instant endDate,
            @RequestParam(required = false) Long staffId) {
        return ResponseEntity.ok(staffService.getCalendarMonthOverview(startDate, endDate, staffId));
    }

    @GetMapping
    public ResponseEntity<List<StaffResponse>> getAllStaffs() {
        return ResponseEntity.ok(this.staffService.getAllStaffs());
    }

    @PutMapping("/{id}")
    public ResponseEntity<StaffResponse> updateStaff(
            @PathVariable Long id,
            @RequestBody @Valid StaffUpdateRequest request) { // 🌟 @ModelAttribute မှ @RequestBody (JSON) သို့ ပြောင်းလဲခြင်း
        return ResponseEntity.ok(this.staffService.updateStaffUser(id, request));
    }

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PostMapping("/assign")
    public ResponseEntity<String> assignLeave(@Valid @RequestBody StaffLeaveRequest request) {
        this.staffService.assignStaffLeave(request);
        return ResponseEntity.ok("Staff leave assigned successfully!");
    }


    @PutMapping({"/{id}/status"})
    public ResponseEntity<StaffResponse> toggleStaffStatus(@PathVariable Long id, @RequestParam boolean enable) {
        return ResponseEntity.ok(this.staffService.toggleStaffStatus(id, enable));
    }

    @PutMapping("/{id}/toggle-availability")
    public ResponseEntity<StaffResponse> toggleStaffAvailability(
            @PathVariable("id") Long staffProfileId,
            @RequestParam("available") boolean available) {

        StaffResponse response = staffService.toggleStaffAvailability(staffProfileId, available);
        return ResponseEntity.ok(response);
    }
}
