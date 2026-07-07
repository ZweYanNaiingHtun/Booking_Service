package com.codingproject.digitalbase.service;

import com.codingproject.digitalbase.dtos.*;
import com.codingproject.digitalbase.enums.BookingStatus;
import com.codingproject.digitalbase.model.Booking;
import com.codingproject.digitalbase.repository.AnalyticsRepository;
import com.codingproject.digitalbase.repository.BookingRepository;
import com.codingproject.digitalbase.repository.StaffProfileRepository;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final BookingRepository bookingRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final AnalyticsRepository analyticsRepository;

    private final ZoneId yangonZone = ZoneId.of("Asia/Yangon");

    // 🌟 Parameter ကြည့်ပြီး Target Date ခွဲခြားပေးမည့် Private Helper Method
    private LocalDate resolveTargetDate(Integer month, Integer year) {
        LocalDate today = LocalDate.now(yangonZone);
        int targetMonth = (month != null) ? month : today.getMonthValue();
        int targetYear = (year != null) ? year : today.getYear();
        return LocalDate.of(targetYear, targetMonth, 1);
    }

    @Override
    public List<TopServiceResponse> getTopServicesTrending(Integer month, Integer year) {
        LocalDate targetDate = resolveTargetDate(month, year);
        List<Booking> allBookings = this.bookingRepository.findAll();

        List<Booking> targetedMonthBookings = allBookings.stream()
                .filter(b -> b.getBookingDate() != null)
                .filter(b -> {
                    LocalDate bDate = b.getBookingDate().atZone(yangonZone).toLocalDate();
                    return bDate.getMonthValue() == targetDate.getMonthValue() && bDate.getYear() == targetDate.getYear();
                })
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED) // Stats နှင့် ကိုက်ညီစေရန်
                .toList();

        long totalServiceCount = targetedMonthBookings.stream()
                .filter(b -> b.getBusinessService() != null)
                .count();

        return targetedMonthBookings.stream()
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
                    return TopServiceResponse.builder()
                            .serviceName(entry.getKey())
                            .count(count)
                            .percentage(Math.round(percentage * 10.0) / 10.0)
                            .build();
                })
                .toList();
    }

    @Override
    public List<StaffPerformance> getStaffPerformanceRanking(Integer month, Integer year) {
        LocalDate targetDate = resolveTargetDate(month, year);
        List<StaffPerformance> basicMetrics = this.analyticsRepository.getStaffPerformanceMetrics();
        List<com.codingproject.digitalbase.model.StaffProfile> staffProfiles = this.staffProfileRepository.findAll();

        return basicMetrics.stream()
                .map(metric -> {
                    staffProfiles.stream()
                            .filter(profile -> profile.getId().equals(metric.getStaffId()))
                            .findFirst()
                            .ifPresent(profile -> {
                                metric.setStaffCode("St-00" + profile.getId());
                                metric.setStaffRole("Nail Artist");
                                metric.setProfileImage("https://api.mari.com/uploads/" + profile.getId() + ".jpg");

                                if (profile.getUser() != null) {
                                    metric.setPhoneNumber(profile.getUser().getPhone());
                                    metric.setEmail(profile.getUser().getEmail());
                                }

                                metric.setDateOfBirth(LocalDate.of(2000, 9, 27));
                                metric.setJoinedDate(LocalDate.of(2026, 1, 1));

                                // Status Mapping
                                boolean isStaffAvailable = profile.isAvailable();
                                boolean hasActiveJob = profile.getAssignedBookings().stream()
                                        .anyMatch(b -> b.getStatus() == BookingStatus.IN_PROGRESS);

                                if (hasActiveJob) {
                                    metric.setStatus("In Progress");
                                } else if (!isStaffAvailable) {
                                    metric.setStatus("Unavailable");
                                } else {
                                    metric.setStatus("Available");
                                }

                                if (profile.getSpecializedServices() != null) {
                                    metric.setSpecializedServiceIds(profile.getSpecializedServices().stream().map(s -> s.getId()).toList());
                                }

                                // 🌟 ရွေးချယ်ထားသော လအလိုက် Revenue & Commission အား စစ်ထုတ်တွက်ချက်ခြင်း
                                double revenue = profile.getAssignedBookings().stream()
                                        .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
                                        .filter(b -> b.getBookingDate() != null)
                                        .filter(b -> {
                                            LocalDate bDate = b.getBookingDate().atZone(yangonZone).toLocalDate();
                                            return bDate.getMonthValue() == targetDate.getMonthValue() && bDate.getYear() == targetDate.getYear();
                                        })
                                        .filter(b -> b.getPayment() != null && b.getPayment().getAmount() != null)
                                        .mapToDouble(b -> b.getPayment().getAmount().doubleValue())
                                        .sum();

                                metric.setTotalRevenue(revenue);
                                metric.setTotalCommission(revenue * 0.20);
                            });
                    return metric;
                })
                .sorted((s1, s2) -> Double.compare(s2.getRatingAverage(), s1.getRatingAverage()))
                .toList();
    }
    @Override
    public DashboardStatsResponse getDashboardStats(Integer month, Integer year) {
        LocalDate targetDate = resolveTargetDate(month, year);
        LocalDate lastMonthDate = targetDate.minusMonths(1); // ပြီးခဲ့သည့်လအား Dynamic တွက်ချက်ခြင်း

        int todayActiveStaff = this.staffProfileRepository.countByIsAvailableTrue();

        // ==================== 💾 OPTIMIZED DB FETCH (ဆရာကြီးအသစ်ထည့်ထားသော Query ကိုသုံးခြင်း) ====================
        // ၁။ ရွေးချယ်ထားသော လအတွက် ဒေတာများကို Database Level မှာတင် ကွက်တိဆွဲယူခြင်း
        List<Booking> currentMonthBookings = this.bookingRepository.findAllByMonthAndYear(
                targetDate.getMonthValue(),
                targetDate.getYear()
        );

        // ၂။ ၎င်း၏ ပြီးခဲ့သည့်လအတွက် ဒေတာများကို Database Level မှာတင် ကွက်တိဆွဲယူခြင်း
        List<Booking> lastMonthBookings = this.bookingRepository.findAllByMonthAndYear(
                lastMonthDate.getMonthValue(),
                lastMonthDate.getYear()
        );

        // ==================== 📊 Target Month Stats တွက်ချက်ခြင်း ====================
        long currentBookingsCount = currentMonthBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
                .count();

        double currentRevenue = currentMonthBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED && b.getPayment() != null && b.getPayment().getAmount() != null)
                .mapToDouble(b -> b.getPayment().getAmount().doubleValue())
                .sum();

        long currentCustomersCount = currentMonthBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED && b.getCustomer() != null)
                .map(b -> b.getCustomer().getId())
                .distinct()
                .count();

        // ==================== 📉 Last Month Stats တွက်ချက်ခြင်း ====================
        long lastBookingsCount = lastMonthBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
                .count();

        double lastRevenue = lastMonthBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED && b.getPayment() != null && b.getPayment().getAmount() != null)
                .mapToDouble(b -> b.getPayment().getAmount().doubleValue())
                .sum();

        long lastCustomersCount = lastMonthBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED && b.getCustomer() != null)
                .map(b -> b.getCustomer().getId())
                .distinct()
                .count();

        // ==================== 📈 Growth % တွက်ချက်ခြင်း ====================
        double bookingsGrowth = calculateGrowth(currentBookingsCount, lastBookingsCount);
        double revenueGrowth = calculateGrowth(currentRevenue, lastRevenue);
        double customersGrowth = calculateGrowth(currentCustomersCount, lastCustomersCount);

        return DashboardStatsResponse.builder()
                .totalBookings(currentBookingsCount)
                .bookingsGrowthPercentage(bookingsGrowth)
                .totalRevenue(currentRevenue)
                .revenueGrowthPercentage(revenueGrowth)
                .todayActiveStaff(todayActiveStaff)
                .staffGrowthPercentage(0.0)
                .totalCustomers(currentCustomersCount)
                .customersGrowthPercentage(customersGrowth)
                .build();
    };

    @Override
    public List<ChartDataPoint> getChartData(String period) {
        LocalDate today = LocalDate.now(yangonZone);
        List<ChartDataPoint> chartData = new ArrayList<>();

        if ("monthly".equalsIgnoreCase(period)) {
            // ==================== 📅 CURRENT MONTH CHART LOGIC ====================
            LocalDateTime startOfMonth = today.withDayOfMonth(1).atStartOfDay();
            LocalDateTime endOfMonth = today.with(TemporalAdjusters.lastDayOfMonth()).atTime(LocalTime.MAX);

            // 🌟 LocalDateTime မှ Instant သို့ yangonZone အသုံးပြု၍ ပြောင်းလဲခြင်း
            Instant startInstant = startOfMonth.atZone(yangonZone).toInstant();
            Instant endInstant = endOfMonth.atZone(yangonZone).toInstant();

            // Repository သို့ Instant Type ဖြင့် ပို့ပေးလိုက်ပါသည်
            List<Booking> bookings = this.bookingRepository.findByBookingDateBetween(startInstant, endInstant);
            int lengthOfMonth = today.lengthOfMonth();

            // 0 ဖြင့် Initialize လုပ်ခြင်း
            for (int i = 1; i <= lengthOfMonth; i++) {
                String dayLabel = String.format("%02d", i);
                chartData.add(ChartDataPoint.builder()
                        .label(dayLabel)
                        .bookingCount(0)
                        .walkInCount(0)
                        .cancelCount(0)
                        .totalBooking(0)
                        .build());
            }

            // ဒေတာများ ဖြည့်သွင်းခြင်း
            for (Booking b : bookings) {
                if (b.getBookingDate() == null) continue;
                int dayOfMonth = b.getBookingDate().atZone(yangonZone).getDayOfMonth();
                ChartDataPoint dataPoint = chartData.get(dayOfMonth - 1);

                populateCounts(b, dataPoint);
            }

        } else {
            // ==================== 📅 CURRENT WEEK CHART LOGIC (DEFAULT) ====================
            LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            LocalDate sunday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

            LocalDateTime startOfWeek = monday.atStartOfDay();
            LocalDateTime endOfWeek = sunday.atTime(LocalTime.MAX);

            // 🌟 LocalDateTime မှ Instant သို့ yangonZone အသုံးပြု၍ ပြောင်းလဲခြင်း
            Instant startInstant = startOfWeek.atZone(yangonZone).toInstant();
            Instant endInstant = endOfWeek.atZone(yangonZone).toInstant();

            // Repository သို့ Instant Type ဖြင့် ပို့ပေးလိုက်ပါသည်
            List<Booking> bookings = this.bookingRepository.findByBookingDateBetween(startInstant, endInstant);

            // Mon မှ Sun အထိ Map တည်ဆောက်ခြင်း
            Map<DayOfWeek, ChartDataPoint> weekMap = new LinkedHashMap<>();
            for (DayOfWeek day : DayOfWeek.values()) {
                String shortName = day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
                weekMap.put(day, ChartDataPoint.builder()
                        .label(shortName)
                        .bookingCount(0)
                        .walkInCount(0)
                        .cancelCount(0)
                        .totalBooking(0)
                        .build());
            }

            // ဒေတာများ ဖြည့်သွင်းခြင်း
            for (Booking b : bookings) {
                if (b.getBookingDate() == null) continue;
                DayOfWeek dayOfWeek = b.getBookingDate().atZone(yangonZone).getDayOfWeek();
                ChartDataPoint dataPoint = weekMap.get(dayOfWeek);

                populateCounts(b, dataPoint);
            }

            chartData.addAll(weekMap.values());
        }

        return chartData;
    }

    // 🛠️ Booking အမျိုးအစားအလိုက် တိုင်ခွဲပေးသည့် Helper Method
    private void populateCounts(Booking b, ChartDataPoint dataPoint) {
        if (b.getStatus() == BookingStatus.CANCELLED) {
            dataPoint.setCancelCount(dataPoint.getCancelCount() + 1);
        } else {
            boolean isWalkIn = b.getCustomer() != null && "CU-WALKIN".equalsIgnoreCase(b.getCustomer().getCode());
            if (isWalkIn) {
                dataPoint.setWalkInCount(dataPoint.getWalkInCount() + 1);
            } else {
                dataPoint.setBookingCount(dataPoint.getBookingCount() + 1);
            }
        }
        dataPoint.setTotalBooking(dataPoint.getBookingCount() + dataPoint.getWalkInCount() + dataPoint.getCancelCount());
    }

    @Override
    public List<TodayBookingResponse> getTodayBookingsFeed() {
        LocalDate today = LocalDate.now(yangonZone);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");

        return this.bookingRepository.findAll().stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED || b.getStatus() == BookingStatus.PENDING)
                .filter(b -> b.getBookingDate() != null && b.getBookingDate().atZone(yangonZone).toLocalDate().equals(today))
                .map(b -> {
                    String serviceName = (b.getBusinessService() != null) ? b.getBusinessService().getName() : "Unknown Service";
                    java.math.BigDecimal servicePrice = (b.getBusinessService() != null) ? b.getBusinessService().getPrice() : java.math.BigDecimal.ZERO;
                    String formattedTime = b.getBookingDate().atZone(yangonZone).toLocalTime().format(timeFormatter);

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
    public StaffPerformance getStaffPerformanceById(Long staffId) {
        LocalDate today = LocalDate.now(yangonZone);
        return this.getStaffPerformanceRanking(today.getMonthValue(), today.getYear()).stream()
                .filter(metric -> metric.getStaffId().equals(staffId))
                .findFirst()
                .orElseThrow(() -> new com.codingproject.digitalbase.exception.ResourceNotFoundException("Staff not found: " + staffId));
    }

    private double calculateGrowth(double current, double previous) {
        if (previous == 0) return current > 0 ? 100.0 : 0.0;
        double growth = ((current - previous) / previous) * 100.0;
        return Math.round(growth * 100.0) / 100.0;
    }

    private boolean isWalkInBooking(Booking b) {
        // Booking ထဲမှာ Customer ရှိရမယ်၊ ပြီးတော့ အဲဒီ Customer ရဲ့ Code က "CU-WALKIN" ဖြစ်ရမယ်
        return b.getCustomer() != null && "CU-WALKIN".equals(b.getCustomer().getCode());
    }
}