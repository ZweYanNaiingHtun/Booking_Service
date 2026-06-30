//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.codingproject.digitalbase.service;

import com.codingproject.digitalbase.dtos.*;
import com.codingproject.digitalbase.enums.BookingStatus;
import com.codingproject.digitalbase.enums.HistoryFilter;
import com.codingproject.digitalbase.model.User;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;

public interface BookingService {
    List<StaffTimeSlotResponse> getAvailableSlotsForStaffAndDate(Long staffUserId, Long serviceId, LocalDate date);

    BookingResponse createCustomerBooking(BookingRequest request);

    BookingResponse createWalkInBooking(WalkInBookingRequest request);

    BookingResponse confirmBooking(Long id);

    BookingResponse cancelBooking(Long id, String cancelledBy, String userEmail);

    void assignStaffToBooking(Long bookingId, @NotNull(
            message = "Staff ID is required"
    ) Long staffId);

    List<User> getAvailableStaffForDateTime(Instant bookingDate);

    BookingResponse acceptBooking(Long id);

    BookingResponse completeBooking(Long id);

    InvoiceResponse generateInvoice(Long id);

    Page<BookingResponse> getMyBookingHistory(int page, int size);

    Page<BookingResponse> getAllBookings(int page, int size);

    List<CustomerStaffResponse> getStaffListForBooking(Long serviceId, Instant bookingDate);

    List<HomeStaffResponse> getStaffListForHomePage();

    List<StaffDutyResponse> getStaffWeeklyDuties(LocalDate selectedDate, BookingStatus status);

    StaffHistoryResponse getStaffWorkHistory(HistoryFilter filter);

    BookingResponse getBookingById(Long id);
}
