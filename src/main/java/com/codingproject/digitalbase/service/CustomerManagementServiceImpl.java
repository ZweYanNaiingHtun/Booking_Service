package com.codingproject.digitalbase.service;

import com.codingproject.digitalbase.dtos.*;
import com.codingproject.digitalbase.model.Booking;
import com.codingproject.digitalbase.model.User;
import com.codingproject.digitalbase.repository.BookingRepository;
import com.codingproject.digitalbase.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List; // 🌟 [ADDED] List တည်ဆောက်ရန်အတွက် လိုအပ်သော Import ထည့်သွင်းထားပါသည်

@Service
@RequiredArgsConstructor
public class CustomerManagementServiceImpl implements CustomerManagementService {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerSummaryResponse> getCustomerList(String search, int page, int size) {
        // စတင်ဖန်တီးသည့် အချိန်အလိုက် နောက်ဆုံးလူကို အပေါ်ဆုံးကပြရန် Sort လုပ်ခြင်း
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<User> customers = userRepository.searchCustomers(search, pageable);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d.M.yyyy").withZone(ZoneId.of("Asia/Yangon"));

        return customers.map(user -> CustomerSummaryResponse.builder()
                .id(user.getId())
                .customerId(user.getCode())
                .customerName(user.getFullName())
                .profilePicture(user.getProfilePicture())
                .email(user.getEmail())
                .phoneNumber(user.getPhone())
                .gender(user.getGender())
                .joinDate(user.getCreatedAt() != null ? formatter.format(user.getCreatedAt()) : "-")
                .bookingCount(user.getBookings() != null ? user.getBookings().size() : 0)
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerDashboardMetrics getCustomerMetrics() {
        long totalCustomers = userRepository.countTotalCustomers();
        long blockedCustomers = userRepository.countBlockedCustomers();

        // Today Bookings အတွက် စံတော်ချိန်သတ်မှတ်ခြင်း
        ZonedDateTime todayYangon = ZonedDateTime.now(ZoneId.of("Asia/Yangon"));
        Instant startOfDay = todayYangon.toLocalDate().atStartOfDay(ZoneId.of("Asia/Yangon")).toInstant();
        Instant endOfDay = todayYangon.toLocalDate().atTime(LocalTime.MAX).atZone(ZoneId.of("Asia/Yangon")).toInstant();
        long todayBookings = bookingRepository.countTodayBookings(startOfDay, endOfDay);

        return CustomerDashboardMetrics.builder()
                .totalCustomers(totalCustomers)
                .todayBookings(todayBookings)
                .blockedCustomers(blockedCustomers)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerDetailResponse getCustomerDetail(Long customerId) {
        // ၁။ Customer (User) အား ရှာဖွေခြင်း
        User user = userRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found with ID: " + customerId));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d.M.yyyy").withZone(ZoneId.of("Asia/Yangon"));

        // ၂။ သက်ဆိုင်ရာ Customer ၏ နောက်ဆုံး Booking ၃ ခုအား ဆွဲထုတ်ခြင်း
        Pageable topThree = PageRequest.of(0, 3);
        List<Booking> topBookings = bookingRepository.findTopRecentBookings(customerId, topThree);

        // ၃။ UI အတိုင်း lowercase ပြောင်းလဲပြီး Recent Bookings List တည်ဆောက်ခြင်း
        List<RecentBookingResponse> recentBookingsDto = topBookings.stream()
                .map(b -> RecentBookingResponse.builder()
                        .serviceName(b.getBusinessService() != null ? b.getBusinessService().getName().toLowerCase() : "-")
                        .bookingDate(b.getCreatedAt() != null ? formatter.format(b.getCreatedAt()) : "-")
                        .build())
                .toList();

        // ၄။ Last Visit ရက်စွဲနှင့် စုစုပေါင်း ဘွတ်ကင်အရေအတွက် တွက်ချက်ခြင်း
        String lastVisitDate = topBookings.isEmpty() ? "-" : formatter.format(topBookings.get(0).getCreatedAt());
        long totalBookingsCount = user.getBookings() != null ? user.getBookings().size() : 0;

        // ၅။ UI ကတ်ပြားကြီးတစ်ခုလုံးစာ အချက်အလက်များအား စုစည်းပေးပို့ခြင်း
        return CustomerDetailResponse.builder()
                .id(user.getId())
                .customerId(user.getCode())
                .customerName(user.getFullName())
                .email(user.getEmail())
                .profilePicture(user.getProfilePicture())
                .joinDate(user.getCreatedAt() != null ? formatter.format(user.getCreatedAt()) : "-")
                .phoneNumber(user.getPhone())
                .status(user.isEnabled() ? "Inprogress" : "Blocked") // UI အစိမ်းရောင် Tag ပုံစံအတိုင်း
                .totalBookings(totalBookingsCount)
                .lastVisit(lastVisitDate)
                .recentBookings(recentBookingsDto)
                .build();
    }
}