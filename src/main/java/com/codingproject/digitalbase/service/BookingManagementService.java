package com.codingproject.digitalbase.service;

import com.codingproject.digitalbase.dtos.BookingDetailResponse;
import com.codingproject.digitalbase.dtos.BookingOverviewWrapper;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

public interface BookingManagementService {
    BookingOverviewWrapper getCustomerBookingsOverview(
            String search, String status, LocalDate startDate, LocalDate endDate, int page, int size);

    BookingDetailResponse getBookingDetails(Long id);
}