//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.codingproject.digitalbase.controller;

import com.codingproject.digitalbase.dtos.StaffDutyResponse;
import com.codingproject.digitalbase.dtos.StaffHistoryResponse;
import com.codingproject.digitalbase.enums.BookingStatus;
import com.codingproject.digitalbase.enums.HistoryFilter;
import com.codingproject.digitalbase.service.BookingService;
import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping({"/api/staff"})
public class StaffDutyController {
    private final BookingService bookingService;

    @GetMapping({"/my-duties"})
    @PreAuthorize("hasAnyRole('STAFF', 'SUPER_ADMIN')")
    public ResponseEntity<List<StaffDutyResponse>> getMyDuties(@RequestParam("date") @DateTimeFormat(iso = ISO.DATE) LocalDate date, @RequestParam("status") BookingStatus status) {
        List<StaffDutyResponse> duties = this.bookingService.getStaffDailyDuties(date, status);
        return ResponseEntity.ok(duties);
    }

    @GetMapping({"/history"})
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    public ResponseEntity<StaffHistoryResponse> getMyHistory(@RequestParam(value = "filter",defaultValue = "TODAY") HistoryFilter filter) {
        StaffHistoryResponse history = this.bookingService.getStaffWorkHistory(filter);
        return ResponseEntity.ok(history);
    }
}
