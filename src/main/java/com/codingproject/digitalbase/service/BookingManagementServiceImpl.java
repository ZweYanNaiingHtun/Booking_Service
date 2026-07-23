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

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
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
            String search, String status, LocalDate startDate, LocalDate endDate,
            int page, int size, String sortBy, String sortDir) {

        List<Booking> allBookings = this.bookingRepository.findAll().stream()
                .filter(b -> b.getCustomer() != null && !"walkin@system.com".equals(b.getCustomer().getEmail()))
                .toList();

        // 🌟 အဆင့် (၁) - Counter တွက်ချက်ခြင်း
        long pending = allBookings.stream().filter(b -> b.getStatus() == BookingStatus.PENDING).count();
        long inProgress = allBookings.stream().filter(b -> b.getStatus() == BookingStatus.IN_PROGRESS).count();
        long confirm = allBookings.stream().filter(b -> b.getStatus() == BookingStatus.CONFIRMED).count();
        long completed = allBookings.stream().filter(b -> b.getStatus() == BookingStatus.COMPLETED).count();
        long rejected = allBookings.stream().filter(b -> b.getStatus() == BookingStatus.CANCELLED).count();

        // 🌟 Sorting Comparator သတ်မှတ်ခြင်း
        Comparator<Booking> bookingComparator;
        switch (sortBy != null ? sortBy.toLowerCase() : "bookingdate") {
            case "id" -> bookingComparator = Comparator.comparing(Booking::getId);
            case "createdat" -> bookingComparator = Comparator.comparing(Booking::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
            case "bookingdate" -> bookingComparator = Comparator.comparing(Booking::getBookingDate, Comparator.nullsLast(Comparator.naturalOrder()));
            default -> bookingComparator = Comparator.comparing(Booking::getBookingDate, Comparator.nullsLast(Comparator.naturalOrder()));
        }

        if ("desc".equalsIgnoreCase(sortDir)) {
            bookingComparator = bookingComparator.reversed();
        }

        // 🌟 အဆင့် (၂) - Date Range, Status Filter နှင့် Sorting ချိန်ညှိခြင်း
        List<Booking> filteredBookings = allBookings.stream()
                .filter(b -> {
                    if (status == null || "ALL".equalsIgnoreCase(status) || status.isBlank()) return true;
                    String normalizedStatus = status.replace(" ", "_").toUpperCase();
                    if ("REJECT".equals(normalizedStatus)) normalizedStatus = "CANCELLED";
                    if ("CONFIRM".equals(normalizedStatus)) normalizedStatus = "CONFIRMED";
                    if ("COMPLETE".equals(normalizedStatus)) normalizedStatus = "COMPLETED";
                    return b.getStatus().name().equals(normalizedStatus);
                })
                .filter(b -> {
                    if (b.getBookingDate() == null) return (startDate == null && endDate == null);
                    LocalDate bDate = b.getBookingDate().atZone(sysZone).toLocalDate();

                    if (startDate != null && endDate != null) {
                        return !bDate.isBefore(startDate) && !bDate.isAfter(endDate);
                    } else if (startDate != null) {
                        return bDate.equals(startDate);
                    } else if (endDate != null) {
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
                .sorted(bookingComparator) // 🌟 In-Memory Sorting ပြုလုပ်ခြင်း
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

        // 🌟 အဆင့် (၄) - Response Mapping
        List<CustomerBookingManagementResponse> responseList = pagedBookings.stream().map(b -> {
            String custName = b.getCustomer() != null ? b.getCustomer().getFullName() : "-";
            String servName = b.getBusinessService() != null ? b.getBusinessService().getName() : "-";

            BigDecimal price = BigDecimal.ZERO;
            if (b.getBusinessService() != null && b.getBusinessService().getPrice() != null) {
                price = b.getBusinessService().getPrice();
            }

            String bDateStr = b.getBookingDate() != null ? dateFormatter.format(b.getBookingDate().atZone(sysZone)) : "-";
            String bTimeStr = b.getBookingDate() != null ? timeFormatter.format(b.getBookingDate().atZone(sysZone)) : "-";

            // 🌟 Staff Name ရွေးချယ်မှု Logic (Assigned Staff -> Requested Staff -> Default)
            String staffNameStr = "Any Available Staff";
            if (b.getAssignedStaff() != null && b.getAssignedStaff().getUser() != null) {
                staffNameStr = b.getAssignedStaff().getUser().getFullName();
            } else if (b.getRequestedStaff() != null && b.getRequestedStaff().getUser() != null) {
                staffNameStr = b.getRequestedStaff().getUser().getFullName();
            }

            String uiStatus = "Pending";
            if (b.getStatus() == BookingStatus.IN_PROGRESS) uiStatus = "In Progress";
            else if (b.getStatus() == BookingStatus.CONFIRMED) uiStatus = "Confirm";
            else if (b.getStatus() == BookingStatus.COMPLETED) uiStatus = "Completed";
            else if (b.getStatus() == BookingStatus.CANCELLED) uiStatus = "Reject";

            return CustomerBookingManagementResponse.builder()
                    .id(b.getId())
                    .code("BK-" + b.getId())
                    .bookingId("BK-" + b.getId())
                    .serviceName(servName)
                    .customerName(custName)
                    .price(price)
                    .bookTime(bTimeStr)
                    .date(bDateStr)
                    .duringTime(b.getBusinessService() != null ? b.getBusinessService().getDurationInMinutes() : 0)
                    .staffName(staffNameStr) // 🌟 ရွေးချယ်ထားသော Staff နာမည် မှန်ကန်စွာ ပေါ်လာမည်ဖြစ်သည်
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
                .bookings(responseList)
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
        String appointmentStr = zonedDateTime.format(formatter);

        String custName = booking.getCustomer() != null ? booking.getCustomer().getFullName() : "-";
        String phone = booking.getCustomer() != null ? booking.getCustomer().getPhone() : "-";

        String serviceName = "-";
        BigDecimal duration = null;
        BigDecimal price = BigDecimal.ZERO; // 🌟 Default အနေဖြင့် ၀ ကျပ် သတ်မှတ်ပါသည်

        if (booking.getBusinessService() != null) {
            serviceName = booking.getBusinessService().getName();

            Integer durationInMins = booking.getBusinessService().getDurationInMinutes();
            if (durationInMins != null) {
                duration = BigDecimal.valueOf(durationInMins);
            }

            // 🌟 'Kyats' စာသားမပါဘဲ Raw Price (BigDecimal) ကို တိုက်ရိုက်ရယူခြင်း
            price = booking.getBusinessService().getPrice();
        }

        String staffStr = "Any Available Staff";
        if (booking.getAssignedStaff() != null) {
            staffStr = booking.getAssignedStaff().getUser().getFullName();
        }

        String uiStatus = "Confirm";
        if (booking.getStatus() == BookingStatus.IN_PROGRESS) uiStatus = "Inprogress";
        else if (booking.getStatus() == BookingStatus.COMPLETED) uiStatus = "Completed";

        return BookingDetailResponse.builder()
                .bookingId("BK-" + booking.getId())
                .customerName(custName)
                .phoneNumber(phone)
                .appointmentDetails(appointmentStr)
                .staffName(staffStr)
                .serviceName(serviceName)
                .duration(duration)
                .price(price) // 🌟 ပြင်ဆင်ပြီး (Raw Numeric Value သာ ဖြစ်ပါသည်)
                .status(uiStatus)
                .build();
    }
}