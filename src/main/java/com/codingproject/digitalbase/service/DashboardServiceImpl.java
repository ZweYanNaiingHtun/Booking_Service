package com.codingproject.digitalbase.service;

import com.codingproject.digitalbase.dtos.*;
import com.codingproject.digitalbase.enums.BookingStatus;
import com.codingproject.digitalbase.model.Booking;
import com.codingproject.digitalbase.model.User;
import com.codingproject.digitalbase.repository.AnalyticsRepository;
import com.codingproject.digitalbase.repository.BookingRepository;
import com.codingproject.digitalbase.repository.StaffProfileRepository;
import com.codingproject.digitalbase.service.DashboardService;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final BookingRepository bookingRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final AnalyticsRepository analyticsRepository; // 🌟 Direct Injection (Middleman မလိုတော့ပါ)

    @Override
    public List<TopServiceResponse> getTopServicesTrending() {
        LocalDate today = LocalDate.now();
        ZoneId sysZone = ZoneId.systemDefault();

        List<Booking> currentMonthBookings = this.bookingRepository.findAll().stream()
                .filter(b -> b.getBookingDate() != null)
                .filter(b -> {
                    LocalDate bDate = b.getBookingDate().atZone(sysZone).toLocalDate();
                    return bDate.getMonth() == today.getMonth() && bDate.getYear() == today.getYear();
                })
                .filter(b -> b.getStatus() != BookingStatus.CANCELLED)
                .toList();

        long totalServiceCount = currentMonthBookings.stream()
                .filter(b -> b.getBusinessService() != null)
                .count();

        return currentMonthBookings.stream()
                .filter(b -> b.getBusinessService() != null)
                .collect(Collectors.groupingBy(
                        b -> b.getBusinessService().getName(),
                        Collectors.counting()
                ))
                .entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                .limit(5)
                .map(entry -> {
                    int count = entry.getValue().intValue();
                    double percentage = totalServiceCount > 0
                            ? ((double) count / totalServiceCount) * 100
                            : 0.0;
                    double roundedPercentage = Math.round(percentage * 10.0) / 10.0;

                    return TopServiceResponse.builder()
                            .serviceName(entry.getKey())
                            .count(count)
                            .percentage(roundedPercentage)
                            .build();
                })
                .toList();
    }

    @Override
    public List<StaffPerformance> getStaffPerformanceRanking() {
        // ၁။ Repository ကနေ အခြေခံ Metrics (Id, Name, Jobs Count, Rating) ကို အရင်ဆွဲယူပါတယ်
        List<StaffPerformance> basicMetrics = this.analyticsRepository.getStaffPerformanceMetrics();

        // ၂။ ကျန်တဲ့ ကိုယ်ရေးအချက်အလက်တွေနဲ့ ဘွတ်ကင်ဒေတာတွေကို ယူဖို့ Staff Profile အားလုံးကို Fetch လုပ်ပါတယ်
        List<com.codingproject.digitalbase.model.StaffProfile> staffProfiles = this.staffProfileRepository.findAll();

        return basicMetrics.stream()
                .map(metric -> {
                    // basicMetrics ထဲက ဝန်ထမ်းတစ်ယောက်ချင်းစီကို StaffProfile Entity နဲ့ လှမ်းချိတ်ပါတယ်
                    staffProfiles.stream()
                            .filter(profile -> profile.getId().equals(metric.getStaffId()))
                            .findFirst()
                            .ifPresent(profile -> {

                                // ======== ဝန်ထမ်းတစ်ဦးချင်းစီ၏ ကိုယ်ရေးအချက်အလက်များ ဖြည့်ခြင်း ========
                                metric.setStaffCode("St-00" + profile.getId());
                                metric.setStaffRole("Nail Artist");
                                metric.setProfileImage("https://api.mari.com/uploads/" + profile.getId() + ".jpg");

                                if (profile.getUser() != null) {
                                    metric.setPhoneNumber(profile.getUser().getPhone());
                                    metric.setEmail(profile.getUser().getEmail());
                                }

                                metric.setDateOfBirth(java.time.LocalDate.of(2000, 9, 27));
                                metric.setJoinedDate(java.time.LocalDate.of(2026, 1, 1));

                                // ======== ဝန်ထမ်းတစ်ဦးချင်းစီ၏ Status ခွဲခြားခြင်း ========
                                boolean isStaffAvailable = profile.isAvailable();
                                boolean hasActiveJob = profile.getAssignedBookings().stream()
                                        .anyMatch(b -> b.getStatus() == com.codingproject.digitalbase.enums.BookingStatus.IN_PROGRESS);

                                if (hasActiveJob) {
                                    metric.setStatus("In Progress");
                                } else if (!isStaffAvailable) {
                                    metric.setStatus("Unavailable");
                                } else {
                                    metric.setStatus("Available");
                                }

                                // ======== 🌟 ဝန်ထမ်းတစ်ဦးချင်းစီ၏ ကျွမ်းကျင်ဝန်ဆောင်မှု (Specialized Services) IDs များ ဖြည့်သွင်းခြင်း ========
                                if (profile.getSpecializedServices() != null) {
                                    List<Long> serviceIds = profile.getSpecializedServices().stream()
                                            .map(service -> service.getId()) // သို့မဟုတ် com.codingproject.digitalbase.model.BusinessService::getId
                                            .toList();
                                    metric.setSpecializedServiceIds(serviceIds); // StaffPerformance DTO ထဲသို့ ID List ထည့်ခြင်း
                                }

                                // ======== ဝန်ထမ်းတစ်ဦးချင်းစီအတွက် (For Each Staff) Revenue & Commission တွက်ချက်ခြင်း ========
                                double revenue = profile.getAssignedBookings().stream()
                                        .filter(b -> b.getStatus() == com.codingproject.digitalbase.enums.BookingStatus.COMPLETED)
                                        .filter(b -> b.getPayment() != null && b.getPayment().getAmount() != null)
                                        .mapToDouble(b -> b.getPayment().getAmount().doubleValue())
                                        .sum();

                                metric.setTotalRevenue(revenue);          // ဝန်ထမ်းတစ်ဦးချင်းစီ၏ Revenue ရလဒ်အမှန်
                                metric.setTotalCommission(revenue * 0.20); // ဝန်ထမ်းတစ်ဦးချင်းစီ၏ Commission ရလဒ်အမှန် (20%)
                            });
                    return metric;
                })
                .sorted((s1, s2) -> {
                    // Rating အကောင်းဆုံးလူကို အရင်စီပြီး၊ Rating တူရင် Completed Jobs အများဆုံးလူကို အရင်ပြပါတယ်
                    int ratingCompare = Double.compare(s2.getRatingAverage(), s1.getRatingAverage());
                    if (ratingCompare != 0) {
                        return ratingCompare;
                    }
                    return Long.compare(s2.getCompletedJobsCount(), s1.getCompletedJobsCount());
                })
                .toList(); // 🌟 ဒေတာကို Wrapper မပါဘဲ Flat List အဖြစ် တိုက်ရိုက် ပြန်ပေးလိုက်ပါတယ်
    }

    @Override
    public StaffPerformance getStaffPerformanceById(Long staffId) {
        // 💡 နဂိုရှိပြီးသား ရလဒ်အစုံထဲကနေ ပေးလိုက်တဲ့ staffId နဲ့ ကွက်တိတူတဲ့ လူတစ်ယောက်တည်းကိုပဲ စစ်ထုတ်ယူပါတယ်
        return this.getStaffPerformanceRanking().stream()
                .filter(metric -> metric.getStaffId().equals(staffId))
                .findFirst()
                .orElseThrow(() -> new com.codingproject.digitalbase.exception.ResourceNotFoundException(
                        "Staff performance data not found for id: " + staffId));
    }
    @Override
    public List<TodayBookingResponse> getTodayBookingsFeed() {
        ZoneId sysZone = ZoneId.systemDefault();
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");

        // 🌟 ၁။ ယနေ့ ရက်စွဲကို စနစ်ရဲ့ Timezone အလိုက် အရင်ရယူထားမည်
        LocalDate today = LocalDate.now(sysZone);

        return this.bookingRepository.findAll().stream()
                // ၂။ Status စစ်ထုတ်ခြင်း (ဆရာကြီး၏ မူလကုဒ်)
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED || b.getStatus() == BookingStatus.PENDING)

                // 🌟 ၃။ အရေးကြီးဆုံးအချက် - ယနေ့ရက်စွဲ ဟုတ်မဟုတ် ထပ်မံ စစ်ထုတ်ခြင်း Filter
                .filter(b -> {
                    if (b.getBookingDate() == null) return false;
                    // Instant အား LocalDate သို့ ပြောင်းလဲခြင်း
                    LocalDate bookingDate = b.getBookingDate().atZone(sysZone).toLocalDate();
                    return bookingDate.equals(today); // ယနေ့ရက်စွဲနှင့် ကွက်တိ တူညီမှသာ သိမ်းမည်
                })
                .map(b -> {
                    String serviceName = (b.getBusinessService() != null) ? b.getBusinessService().getName() : "Unknown Service";
                    java.math.BigDecimal servicePrice = (b.getBusinessService() != null) ? b.getBusinessService().getPrice() : java.math.BigDecimal.ZERO;

                    String formattedTime = "10:00 AM";
                    if (b.getBookingDate() != null) {
                        formattedTime = b.getBookingDate().atZone(sysZone).toLocalTime().format(timeFormatter);
                    }

                    String staffName = "Unassigned";
                    if (b.getAssignedStaff() != null && b.getAssignedStaff().getUser() != null) {
                        staffName = b.getAssignedStaff().getUser().getFullName();
                    }

                    return TodayBookingResponse.builder()
                            .id("BK-" + b.getId())
                            .staffName(staffName)
                            .serviceNames(serviceName)
                            .bookingTime(formattedTime)
                            .price(servicePrice)
                            .status(b.getStatus().name())
                            .build();
                })
                .toList();
    }

    @Override
    public DashboardStatsResponse getDashboardStats() {
        List<Booking> allBookings = this.bookingRepository.findAll();
        LocalDate today = LocalDate.now();
        ZoneId sysZone = ZoneId.systemDefault();

        int todayActiveStaff = this.staffProfileRepository.countByIsAvailableTrue();

        // 🌟 အမြဲတမ်း လက်ရှိလ (Current Month) အတွက် ဒေတာကိုပဲ သီးသန့်စစ်ထုတ်ယူခြင်း
        List<Booking> currentMonthBookings = allBookings.stream()
                .filter(b -> b.getBookingDate() != null)
                .filter(b -> {
                    LocalDate bDate = b.getBookingDate().atZone(sysZone).toLocalDate();
                    return bDate.getMonth() == today.getMonth() && bDate.getYear() == today.getYear();
                })
                .toList();

        long totalBookings = currentMonthBookings.stream()
                .filter(b -> b.getStatus() != BookingStatus.CANCELLED)
                .count();

        double totalRevenue = currentMonthBookings.stream()
                .filter(b -> b.getStatus() != BookingStatus.CANCELLED)
                .filter(b -> b.getPayment() != null && b.getPayment().getAmount() != null)
                .mapToDouble(b -> b.getPayment().getAmount().doubleValue())
                .sum();

        long totalCustomers = currentMonthBookings.stream()
                .filter(b -> b.getStatus() != BookingStatus.CANCELLED)
                .map(Booking::getCustomer)
                .filter(java.util.Objects::nonNull)
                .map(User::getId)
                .distinct()
                .count();

        return DashboardStatsResponse.builder()
                .totalBookings(totalBookings)
                .totalRevenue(totalRevenue)
                .todayActiveStaff(todayActiveStaff)
                .totalCustomers(totalCustomers)
                .build();
    }

    @Override
    public List<ChartDataPoint> getChartData(String period) {
        List<Booking> allBookings = this.bookingRepository.findAll();
        LocalDate today = LocalDate.now();
        ZoneId sysZone = ZoneId.systemDefault();

        // ==================== 🟢 MONTHLY CHART LOGIC ====================
        if ("monthly".equalsIgnoreCase(period)) {
            int currentYear = today.getYear();

            List<Booking> currentYearBookings = allBookings.stream()
                    .filter(b -> b.getBookingDate() != null)
                    .filter(b -> b.getBookingDate().atZone(sysZone).toLocalDate().getYear() == currentYear)
                    .toList();

            return Arrays.stream(Month.values())
                    .map(month -> {
                        List<Booking> monthBookings = currentYearBookings.stream()
                                .filter(b -> b.getBookingDate().atZone(sysZone).toLocalDate().getMonth() == month)
                                .toList();

                        int activeCount = (int) monthBookings.stream()
                                .filter(b -> b.getStatus() != BookingStatus.CANCELLED)
                                .count();

                        int cancelCount = (int) monthBookings.stream()
                                .filter(b -> b.getStatus() == BookingStatus.CANCELLED)
                                .count();

                        return ChartDataPoint.builder()
                                .label(month.name()) // "JANUARY", "FEBRUARY", etc.
                                .bookingCount(activeCount)
                                .cancelCount(cancelCount)
                                .build();
                    })
                    .toList();

            // ==================== 🔵 WEEKLY CHART LOGIC ====================
        } else {
            java.time.temporal.TemporalAdjuster startOfWeekAdjuster = java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY);
            java.time.temporal.TemporalAdjuster endOfWeekAdjuster = java.time.temporal.TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY);

            LocalDate startOfWeek = today.with(startOfWeekAdjuster);
            LocalDate endOfWeek = today.with(endOfWeekAdjuster);

            List<Booking> weeklyBookings = allBookings.stream()
                    .filter(b -> b.getBookingDate() != null)
                    .filter(b -> {
                        LocalDate bDate = b.getBookingDate().atZone(sysZone).toLocalDate();
                        return !bDate.isBefore(startOfWeek) && !bDate.isAfter(endOfWeek);
                    })
                    .toList();

            return Arrays.stream(DayOfWeek.values())
                    .map(day -> {
                        List<Booking> dayBookings = weeklyBookings.stream()
                                .filter(b -> b.getBookingDate().atZone(sysZone).toLocalDate().getDayOfWeek() == day)
                                .toList();

                        int activeCount = (int) dayBookings.stream()
                                .filter(b -> b.getStatus() != BookingStatus.CANCELLED)
                                .count();

                        int cancelCount = (int) dayBookings.stream()
                                .filter(b -> b.getStatus() == BookingStatus.CANCELLED)
                                .count();

                        return ChartDataPoint.builder()
                                .label(day.name()) // "MONDAY", "TUESDAY", etc.
                                .bookingCount(activeCount)
                                .cancelCount(cancelCount)
                                .build();
                    })
                    .toList();
        }
    }
}