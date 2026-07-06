package com.codingproject.digitalbase.service;

import com.codingproject.digitalbase.dtos.BookingDetailResponse;
import com.codingproject.digitalbase.dtos.BookingOverviewWrapper;
import com.codingproject.digitalbase.dtos.CustomerBookingManagementResponse; // 🌟 DTO အသစ်ကို Import လုပ်ပါသည်
import com.codingproject.digitalbase.enums.BookingStatus;
import com.codingproject.digitalbase.exception.ResourceNotFoundException;
import com.codingproject.digitalbase.model.Booking;
import com.codingproject.digitalbase.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingManagementServiceImpl implements BookingManagementService {

    private final BookingRepository bookingRepository;
    private final ZoneId sysZone = ZoneId.systemDefault();

    @Override
    @Transactional(readOnly = true)
    public BookingOverviewWrapper getCustomerBookingsOverview(
            String search, String status, LocalDate startDate, LocalDate endDate, int page, int size) {

        List<Booking> allBookings = this.bookingRepository.findAll().stream()
                .filter(b -> b.getCustomer() != null && !"walkin@system.com".equals(b.getCustomer().getEmail()))
                // 💡 သို့မဟုတ် b.getCustomer().getCode() ကို သုံးချင်ပါက: !"CU-WALKIN".equals(b.getCustomer().getCode())
                .toList();

        // 🌟 အဆင့် (၁) - Counter တွက်ချက်ခြင်း
        long pending = allBookings.stream().filter(b -> b.getStatus() == BookingStatus.PENDING).count();
        long inProgress = allBookings.stream().filter(b -> b.getStatus() == BookingStatus.IN_PROGRESS).count();
        long confirm = allBookings.stream().filter(b -> b.getStatus() == BookingStatus.CONFIRMED).count();
        long completed = allBookings.stream().filter(b -> b.getStatus() == BookingStatus.COMPLETED).count();
        long rejected = allBookings.stream().filter(b -> b.getStatus() == BookingStatus.CANCELLED).count();

        // 🌟 အဆင့် (၂) - Date Range နှင့် Status Filter ချိန်ညှိခြင်း
        List<Booking> filteredBookings = allBookings.stream()
                .filter(b -> {
                    if (status == null || "ALL".equalsIgnoreCase(status) || status.isBlank()) return true;
                    String normalizedStatus = status.replace(" ", "_").toUpperCase();
                    if ("REJECT".equals(normalizedStatus)) normalizedStatus = "CANCELLED"; // UI Counter နှင့် Backend Enum ချိတ်ဆက်မှု ညှိခြင်း
                    if ("CONFIRM".equals(normalizedStatus)) normalizedStatus = "CONFIRMED";
                    if ("COMPLETE".equals(normalizedStatus)) normalizedStatus = "COMPLETED";
                    return b.getStatus().name().equals(normalizedStatus);
                })
                .filter(b -> {
                    if (b.getBookingDate() == null) return (startDate == null && endDate == null);
                    LocalDate bDate = b.getBookingDate().atZone(sysZone).toLocalDate();

                    if (startDate != null && endDate != null) {
                        // ၁။ ရက်စွဲနှစ်ခုလုံး ပါလာလျှင် ကြားကာလအပိုင်းအခြားအတိုင်း စစ်ထုတ်မည်
                        return !bDate.isBefore(startDate) && !bDate.isAfter(endDate);
                    } else if (startDate != null) {
                        // ၂။ 💡 ဆရာကြီး လိုချင်သလို endDate မပါဘဲ startDate တစ်ခုတည်း (ဥပမာ 2026-07-07) ပို့လာလျှင်
                        // ၎င်းနေ့ရက်တစ်ရက်တည်းနှင့် ကွက်တိ တူညီသော ဘွတ်ကင်များကိုသာ ပြသမည်
                        return bDate.equals(startDate);
                    } else if (endDate != null) {
                        // ၃။ endDate တစ်ခုတည်း ပါလာလျှင် ၎င်းရက်မတိုင်ခင်အထိ ပြသမည်
                        return !bDate.isAfter(endDate);
                    }
                    return true;
                })
                .filter(b -> {
                    if (search == null || search.isBlank()) return true;
                    String s = search.toLowerCase();
                    return ("BK-" + b.getId()).toLowerCase().contains(s) ||
                            (b.getBusinessService() != null && b.getBusinessService().getName().toLowerCase().contains(s)) ||
                            (b.getCustomer() != null && b.getCustomer().getFullName().toLowerCase().contains(s));
                })
                .toList();

        // 🌟 အဆင့် (၃) - Pagination တွက်ချက်ခြင်း
        int totalElements = filteredBookings.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);
        List<Booking> pagedBookings = filteredBookings.subList(fromIndex, toIndex);

        // UI Formatter များ ပြင်ဆင်ခြင်း
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);
        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);

        // 🌟 အဆင့် (၄) - Response Mapping (Type အမှားအား ပြင်ဆင်ထားပါသည်)
        List<CustomerBookingManagementResponse> responseList = pagedBookings.stream().map(b -> {
            String custName = b.getCustomer() != null ? b.getCustomer().getFullName() : "-";
            String servName = b.getBusinessService() != null ? b.getBusinessService().getName() : "-";

            String priceStr = "0 MMK";
            if (b.getBusinessService() != null && b.getBusinessService().getPrice() != null) {
                priceStr = numberFormat.format(b.getBusinessService().getPrice()) + " MMK";
            }

            String bDateStr = b.getBookingDate() != null ? dateFormatter.format(b.getBookingDate().atZone(sysZone)) : "-";
            String bTimeStr = b.getBookingDate() != null ? timeFormatter.format(b.getBookingDate().atZone(sysZone)) : "-";

            String staffNameStr = "Any Available Staff";
            if (b.getAssignedStaff() != null && b.getAssignedStaff().getUser() != null) {
                staffNameStr = b.getAssignedStaff().getUser().getFullName();
            }

            String uiStatus = "Pending";
            if (b.getStatus() == BookingStatus.IN_PROGRESS) uiStatus = "In Progress";
            else if (b.getStatus() == BookingStatus.CONFIRMED) uiStatus = "Confirm";
            else if (b.getStatus() == BookingStatus.COMPLETED) uiStatus = "Completed";
            else if (b.getStatus() == BookingStatus.CANCELLED) uiStatus = "Reject";

            // 🌟 CustomerBookingManagementResponse ဖြင့် ကွက်တိ Build လုပ်ခြင်း
            return CustomerBookingManagementResponse.builder()
                    .id(b.getId())
                    .code("BK-" + b.getId())            // code field သို့ ထည့်သွင်းခြင်း
                    .bookingId("BK-" + b.getId())       // 💡 Frontend မှ bookingId တောင်းလျှင်လည်း အဆင်ပြေစေရန် ဖြည့်စွက်ခြင်း
                    .serviceName(servName)
                    .customerName(custName)
                    .price(priceStr)
                    .bookTime(bTimeStr)
                    .date(bDateStr)
                    .duringTime("1 hr 30 mins")         // b.getBusinessService().getDuration() သို့ ပြောင်းလဲနိုင်ပါသည်
                    .staffName(staffNameStr)
                    .status(uiStatus)
                    .build();
        }).collect(Collectors.toList());

        // Wrapper ဖြင့် အားလုံးကို ထုပ်ပိုးပြီး Return ပြန်ပေးခြင်း
        return BookingOverviewWrapper.builder()
                .pendingCount(pending)
                .inProgressCount(inProgress)
                .confirmCount(confirm)
                .completedCount(completed)
                .rejectedCount(rejected)
                .bookings(responseList) // 🌟 ပြင်ဆင်ပြီးသား List အား ပို့ဆောင်ခြင်း
                .currentPage(page)
                .totalPages(totalPages)
                .totalElements(totalElements)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public BookingDetailResponse getBookingDetails(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));

        // Instant အား ရန်ကုန်စံတော်ချိန်ပြောင်း၍ Format လုပ်ခြင်း
        ZonedDateTime zonedDateTime = booking.getBookingDate().atZone(ZoneId.of("Asia/Yangon"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, MMMM dd . h:mm a", Locale.ENGLISH);
        String appointmentStr = zonedDateTime.format(formatter); // Saturday, July 13 . 9:00 AM

        String custName = booking.getCustomer() != null ? booking.getCustomer().getFullName() : "-";
        String phone = booking.getCustomer() != null ? booking.getCustomer().getPhone() : "-"; // ဖုန်းကော်လံအမည် စစ်ဆေးပါ

        String serviceDetails = "-";
        String priceStr = "0 Kyats";
        if (booking.getBusinessService() != null) {
            serviceDetails = booking.getBusinessService().getName() + " (" + booking.getBusinessService().getDurationInMinutes() + " mins)";
            java.text.NumberFormat nf = java.text.NumberFormat.getNumberInstance(Locale.US);
            priceStr = nf.format(booking.getBusinessService().getPrice()) + " Kyats";
        }

        String staffStr = "Any Available Staff";
        if (booking.getAssignedStaff() != null) {
            staffStr = "Nail artist, " + booking.getAssignedStaff().getUser().getFullName();
        }

        // UI Overlay ခေါင်းစဉ်နှင့် ကိုက်ညီအောင် Status စာသားညှိခြင်း
        String uiStatus = "Confirm";
        if (booking.getStatus() == BookingStatus.IN_PROGRESS) uiStatus = "Inprogress";
        else if (booking.getStatus() == BookingStatus.COMPLETED) uiStatus = "Completed";

        return BookingDetailResponse.builder()
                .bookingId("BK-" + booking.getId())
                .customerName(custName)
                .phoneNumber(phone)
                .appointmentDetails(appointmentStr)
                .staffName(staffStr)
                .serviceNameAndDuration(serviceDetails)
                .totalCharges(priceStr)
                .status(uiStatus)
                .build();
    }
}