package com.codingproject.digitalbase.controller;

import com.codingproject.digitalbase.dtos.*;
import com.codingproject.digitalbase.service.BookingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.security.Principal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping({"/api/bookings"})
public class BookingController {
    private final BookingService bookingService;

    @PostMapping({"/customer"})
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<BookingResponse> createCustomerBooking(@RequestBody @Valid BookingRequest request) {
        return ResponseEntity.ok(this.bookingService.createCustomerBooking(request));
    }

    @PostMapping({"/walk-in"})
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<BookingResponse> createWalkInBooking(@RequestBody @Valid WalkInBookingRequest request) {
        return ResponseEntity.ok(this.bookingService.createWalkInBooking(request));
    }

    @GetMapping({"/my-history"})
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Page<BookingResponse>> getMyBookingHistory(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(this.bookingService.getMyBookingHistory(page, size));
    }

    @PutMapping({"/{id}/confirm"})
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<BookingResponse> confirmBooking(@PathVariable Long id) {
        return ResponseEntity.ok(this.bookingService.confirmBooking(id));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'STAFF')")
    public ResponseEntity<BookingResponse> cancelAdminBooking(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> requestBody) {

        // Frontend (React) ဘက်က { "reason": "..." } ဆိုပြီး လှမ်းပို့လာမည့်စာသားကို ဖတ်ခြင်း
        String reason = (requestBody != null) ? requestBody.get("reason") : null;

        // Service ထံသို့ reason ပါ တစ်ပါတည်း လွှဲပေးလိုက်ခြင်း
        return ResponseEntity.ok(this.bookingService.cancelBookingByAdmin(id, "ADMIN", null, reason));
    }

    @PutMapping({"/{id}/cancel"})
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<BookingResponse> cancelCustomerBooking(@PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(this.bookingService.cancelBookingByCustomer(id, "CUSTOMER", principal.getName()));
    }

    @GetMapping({"/all"})
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'STAFF')")
    public ResponseEntity<Page<BookingResponse>> getAllBookings(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(this.bookingService.getAllBookings(page, size));
    }

    @PutMapping({"/{bookingId}/assign"})
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> assignStaff(@PathVariable Long bookingId, @RequestBody @Valid StaffAssignmentRequest request) {
        this.bookingService.assignStaffToBooking(bookingId, request.getStaffId());
        return ResponseEntity.ok("Staff assigned and notification sent successfully.");
    }
    @GetMapping("/home-staffs")
    public ResponseEntity<List<HomeStaffResponse>> getStaffListForHomePage() {
        return ResponseEntity.ok(bookingService.getStaffListForHomePage());
    }

    @GetMapping({"/available-staff"})
    public ResponseEntity<List<AvailableStaffResponse>> getAvailableStaff(@RequestParam("date") String dateStr) {
        Instant bookingDate = Instant.parse(dateStr);
        List<AvailableStaffResponse> availableStaff = this.bookingService.getAvailableStaffForDateTime(bookingDate);
        return ResponseEntity.ok(availableStaff);
    }

    @PutMapping({"/{id}/accept"})
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'STAFF')")
    public ResponseEntity<BookingResponse> acceptBooking(@PathVariable Long id) {
        return ResponseEntity.ok(this.bookingService.acceptBooking(id));
    }

    @PutMapping({"/{id}/complete"})
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'STAFF')")
    public ResponseEntity<BookingResponse> completeBooking(@PathVariable Long id) {
        return ResponseEntity.ok(this.bookingService.completeBooking(id));
    }

    @GetMapping({"/{id}/invoice"})
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'STAFF', 'CUSTOMER')")
    public ResponseEntity<InvoiceResponse> getInvoice(@PathVariable Long id) {
        return ResponseEntity.ok(this.bookingService.generateInvoice(id));
    }

    @GetMapping({"/available-staffs"})
    @PreAuthorize("hasAnyRole('CUSTOMER', 'SUPER_ADMIN')")
    public ResponseEntity<List<CustomerStaffResponse>> getAvailableStaffsForBooking(@RequestParam(required = false) Long serviceId, @RequestParam @NotNull Instant bookingDate) {
        List<CustomerStaffResponse> staffs = this.bookingService.getStaffListForBooking(serviceId, bookingDate);
        return ResponseEntity.ok(staffs);
    }

    @GetMapping({"/available-slots"})
    @PreAuthorize("hasAnyRole('CUSTOMER', 'SUPER_ADMIN', 'STAFF')")
    public ResponseEntity<List<StaffTimeSlotResponse>> getAvailableSlots(@RequestParam Long staffId, @RequestParam Long serviceId, @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate date) {
        List<StaffTimeSlotResponse> slots = this.bookingService.getAvailableSlotsForStaffAndDate(staffId, serviceId, date);
        return ResponseEntity.ok(slots);
    }

    @GetMapping({"/{id}"})
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'STAFF', 'CUSTOMER')")
    public ResponseEntity<BookingResponse> getBookingById(@PathVariable Long id) {
        return ResponseEntity.ok(this.bookingService.getBookingById(id));
    }
}
