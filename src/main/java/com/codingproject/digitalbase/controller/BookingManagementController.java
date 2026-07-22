package com.codingproject.digitalbase.controller;

import com.codingproject.digitalbase.dtos.BookingDetailResponse;
import com.codingproject.digitalbase.dtos.BookingOverviewWrapper;
import com.codingproject.digitalbase.service.BookingManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/booking-management")
@CrossOrigin(origins = "*")
public class BookingManagementController {

    private final BookingManagementService bookingManagementService;

    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @GetMapping("/overview")
    public ResponseEntity<BookingOverviewWrapper> getBookingOverview(
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "ALL") String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "bookingDate") String sortBy, // 🌟 Default အနေဖြင့် bookingDate ဖြင့် စီပါမည်
            @RequestParam(defaultValue = "desc") String sortDir) {      // 🌟 Default အနေဖြင့် desc (အသစ်ဆုံး/အနီးဆုံး) ထိပ်ဆုံးတင်ပါမည်

        BookingOverviewWrapper data = this.bookingManagementService.getCustomerBookingsOverview(
                search, status, startDate, endDate, page, size, sortBy, sortDir);

        return ResponseEntity.ok(data);
    }

    @GetMapping("/{id}/details")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<BookingDetailResponse> getBookingDetails(@PathVariable Long id) {
        return ResponseEntity.ok(this.bookingManagementService.getBookingDetails(id)); // bookingService သို့မဟုတ် မိမိ service နာမည်ပြောင်းပါ
    }
}