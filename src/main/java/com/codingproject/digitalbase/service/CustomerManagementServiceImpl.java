package com.codingproject.digitalbase.service;

import com.codingproject.digitalbase.dtos.*;
import com.codingproject.digitalbase.exception.ResourceNotFoundException;
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

import java.math.BigDecimal;
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
    @Override
    @Transactional(readOnly = true)
    public WalkInDetailResponse getWalkInDetail(Long bookingId) {
        // ၁။ သက်ဆိုင်ရာ Walk-in Booking အား ID ဖြင့် ရှာဖွေခြင်း
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Walk-in booking not found with ID: " + bookingId));

        // ၂။ Time Zone နှင့် Formatter များ သတ်မှတ်ခြင်း
        ZoneId zoneId = ZoneId.of("Asia/Yangon");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(zoneId);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a").withZone(zoneId);

        // ၃။ UI ပုံစံအတိုင်း "CUST-001" ဖြစ်လာစေရန် Format ပြောင်းခြင်း
        String formattedWalkInId = String.format("CUST-%03d", booking.getId());

        // ၄။ Service Duration အား မိနစ်မှ နာရီပုံစံ (e.g., "2 Hours") သို့ ပြောင်းလဲခြင်း
        Integer minutes = (booking.getBusinessService() != null) ? booking.getBusinessService().getDurationInMinutes() : 60;
        double hours = minutes / 60.0;
        String durationStr = (hours % 1 == 0) ? (int)hours + " Hours" : hours + " Hours";

        // ၅။ Payment တန်ဖိုးများ ဆွဲထုတ်ခြင်း (Null ဖြစ်နေပါက 0 သတ်မှတ်မည်)
        BigDecimal basicAmt = BigDecimal.ZERO;
        BigDecimal extraAmt = BigDecimal.ZERO;
        BigDecimal totalAmt = BigDecimal.ZERO;

        if (booking.getPayment() != null) {
            basicAmt = booking.getPayment().getBaseAmount() != null ? booking.getPayment().getBaseAmount() : BigDecimal.ZERO;
            extraAmt = booking.getPayment().getExtraAmount() != null ? booking.getPayment().getExtraAmount() : BigDecimal.ZERO;
            totalAmt = booking.getPayment().getAmount() != null ? booking.getPayment().getAmount() : BigDecimal.ZERO;
        }

        // ၆။ UI ကတ်ပြားလေး (Modal) အတွက် ဒေတာများ စုစည်းပေးပို့ခြင်း
        return WalkInDetailResponse.builder()
                .id(booking.getId())
                .walkInId(formattedWalkInId)
                .date(booking.getBookingDate() != null ? dateFormatter.format(booking.getBookingDate()) : "-")
                .staffName(booking.getAssignedStaff() != null ? booking.getAssignedStaff().getUser().getFullName() : "-")
                .startTime(booking.getBookingDate() != null ? timeFormatter.format(booking.getBookingDate()) : "-")
                .serviceName(booking.getBusinessService() != null ? booking.getBusinessService().getName() : "-")
                .duration(durationStr)
                .basicAmount(basicAmt)
                .extraAmount(extraAmt)
                .totalCharges(totalAmt)
                .extraChargesNote(booking.getNotes() != null ? booking.getNotes() : "") // Extra Charges Note နေရာတွင် ပြရန်
                .build();
    }
    @Override
    @Transactional(readOnly = true)
    public Page<WalkInSummaryResponse> getWalkInList(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Booking> walkInBookings = bookingRepository.findWalkInBookings(pageable);

        // အချိန်နှင့် ရက်စွဲ Format သတ်မှတ်ခြင်း
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("d/M/yyyy").withZone(ZoneId.of("Asia/Yangon"));
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a").withZone(ZoneId.of("Asia/Yangon"));

        return walkInBookings.map(b -> {
            // 🌟 ၁။ UI ထဲကလို "CUS-001" ပုံစံထွက်အောင် Booking ID ကို သုံး၍ Format ပြောင်းခြင်း
            String formattedWalkInId = String.format("CUS - %03d", b.getId());

            // ၂။ Service Duration အား Hours ဖြင့် ပြောင်းလဲတွက်ချက်ခြင်း
            Integer minutes = (b.getBusinessService() != null) ? b.getBusinessService().getDurationInMinutes() : 60;
            double hours = minutes / 60.0;
            String totalTimeStr = (hours % 1 == 0) ? (int)hours + " hours" : hours + " hours";

            // ၃။ Payment အချက်အလက်များအား UI Tag ပုံစံအတိုင်း စာသားစုစည်းခြင်း
            String amountDisplay = "0 MMK";
            BigDecimal totalAmt = BigDecimal.ZERO;

            if (b.getPayment() != null) {
                totalAmt = b.getPayment().getAmount();
                BigDecimal extra = b.getPayment().getExtraAmount();

                if (extra != null && extra.compareTo(BigDecimal.ZERO) > 0) {
                    amountDisplay = String.format("%.0f MMK +%.0fk Extra", b.getPayment().getBaseAmount(), extra);
                } else {
                    amountDisplay = String.format("%.0f MMK", totalAmt);
                }
            }

            return WalkInSummaryResponse.builder()
                    .id(b.getId())
                    .walkInId(formattedWalkInId)
                    .serviceName(b.getBusinessService() != null ? b.getBusinessService().getName() : "-")
                    .staffName(b.getAssignedStaff() != null ? b.getAssignedStaff().getUser().getFullName() : "-")
                    .date(b.getBookingDate() != null ? dateFormatter.format(b.getBookingDate()) : "-")
                    .startTime(b.getBookingDate() != null ? timeFormatter.format(b.getBookingDate()) : "-")
                    .totalTime(totalTimeStr)
                    .totalAmountDisplay(amountDisplay)
                    .totalAmount(totalAmt)
                    .build();
        });
    }
}