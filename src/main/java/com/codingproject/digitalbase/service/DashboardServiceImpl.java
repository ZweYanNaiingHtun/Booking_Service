package com.codingproject.digitalbase.service;

import com.codingproject.digitalbase.dtos.*;
import com.codingproject.digitalbase.enums.BookingStatus;
import com.codingproject.digitalbase.model.Booking;
import com.codingproject.digitalbase.model.BusinessService;
import com.codingproject.digitalbase.model.StaffLeave;
import com.codingproject.digitalbase.model.User;
import com.codingproject.digitalbase.repository.AnalyticsRepository;
import com.codingproject.digitalbase.repository.BookingRepository;
import com.codingproject.digitalbase.repository.StaffLeaveRepository;
import com.codingproject.digitalbase.repository.StaffProfileRepository;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final BookingRepository bookingRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final AnalyticsRepository analyticsRepository;
    private final StaffLeaveRepository staffLeaveRepository;

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

        // ၁။ သတ်မှတ်ထားသော လနှင့် နှစ်အလိုက် Completed ဖြစ်သွားသော Booking များကို စစ်ထုတ်ခြင်း
        List<Booking> targetedMonthBookings = allBookings.stream()
                .filter(b -> b.getBookingDate() != null)
                .filter(b -> {
                    LocalDate bDate = b.getBookingDate().atZone(yangonZone).toLocalDate();
                    return bDate.getMonthValue() == targetDate.getMonthValue() && bDate.getYear() == targetDate.getYear();
                })
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
                .filter(b -> b.getBusinessService() != null)
                .toList();

        // ၂။ သတ်မှတ်ထားသော လအတွက် စုစုပေါင်း ရရှိသော ဝင်ငွေ (Total Monthly Revenue) အား တွက်ချက်ခြင်း
        double totalMonthlyRevenue = targetedMonthBookings.stream()
                .mapToDouble(b -> b.getBusinessService().getPrice() != null
                        ? b.getBusinessService().getPrice().doubleValue()
                        : 0.0)
                .sum();

        // 🌟 ၃။ တွက်ချက်မှုအသစ်: လစဉ် စုစုပေါင်း Booking အရေအတွက် (Total Monthly Bookings Count)
        int totalMonthlyBookings = targetedMonthBookings.size();

        // ၄။ Business Service အလိုက် Grouping ဖွဲ့ပြီး Detail Stats များ တွက်ချက်ခြင်း
        Map<BusinessService, List<Booking>> bookingsByService = targetedMonthBookings.stream()
                .collect(Collectors.groupingBy(Booking::getBusinessService));

        return bookingsByService.entrySet().stream()
                .map(entry -> {
                    BusinessService service = entry.getKey();
                    List<Booking> serviceBookings = entry.getValue();

                    // 🎯 Walk-in Booking များကို ရေတွက်ခြင်း (Customer Code: "CU-WALKIN")
                    int walkInCount = (int) serviceBookings.stream()
                            .filter(b -> b.getCustomer() != null && "CU-WALKIN".equals(b.getCustomer().getCode()))
                            .count();

                    // 🎯 Normal Appointment များကို ရေတွက်ခြင်း (Total - Walk-in)
                    int totalCount = serviceBookings.size();
                    int appointmentCount = totalCount - walkInCount;

                    // 🎯 BigDecimal မှ doubleValue() သို့ ပြောင်းလဲ၍ စုစုပေါင်း Service ဝင်ငွေ တွက်ချက်ခြင်း
                    double serviceRevenue = serviceBookings.stream()
                            .mapToDouble(b -> b.getBusinessService().getPrice() != null
                                    ? b.getBusinessService().getPrice().doubleValue()
                                    : 0.0)
                            .sum();

                    // 🎯 ဝင်ငွေ ရာခိုင်နှုန်း ရှာဖွေခြင်း (% of Revenue)
                    double revenuePercentage = totalMonthlyRevenue > 0
                            ? (serviceRevenue / totalMonthlyRevenue) * 100
                            : 0.0;

                    // 🎯 🌟 တွက်ချက်မှုအသစ်: အရေအတွက် ရာခိုင်နှုန်း ရှာဖွေခြင်း (% of Bookings for Pie Chart)
                    double countPercentage = totalMonthlyBookings > 0
                            ? ((double) totalCount / totalMonthlyBookings) * 100
                            : 0.0;

                    double singlePrice = service.getPrice() != null ? service.getPrice().doubleValue() : 0.0;

                    return TopServiceResponse.builder()
                            .serviceName(service.getName())
                            .appointment(appointmentCount)
                            .walkIn(walkInCount)
                            .price(singlePrice)
                            .revenue(serviceRevenue)
                            .totalCount(totalCount)
                            .revenuePercentage(Math.round(revenuePercentage * 10.0) / 10.0) // ဒသမ ၁ နေရာထိ ဖြတ်ခြင်း
                            .countPercentage(Math.round(countPercentage * 10.0) / 10.0)     // 🌟 ဒသမ ၁ နေရာထိ ဖြတ်ခြင်း
                            .build();
                })
                // ၅။ Booked Count အများဆုံး (Total Count Descending) ဖြင့် Sort စီပြီး Top 5 ကို ဆွဲထုတ်ခြင်း
                .sorted((s1, s2) -> Integer.compare(s2.getTotalCount(), s1.getTotalCount()))
                .limit(5)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public StaffOverviewWrapper getStaffPerformanceRanking(Integer month, Integer year) {
        LocalDate targetDate = resolveTargetDate(month, year);
        List<StaffPerformance> basicMetrics = this.analyticsRepository.getStaffPerformanceMetrics();
        List<com.codingproject.digitalbase.model.StaffProfile> staffProfiles = this.staffProfileRepository.findAll();

        // 🌟 [ADDED] ယနေ့ ခွင့် (Leave) / Day Off ယူထားသော Staff များကို DB ထဲမှ Fetch ပြုလုပ်ခြင်း
        Instant today = Instant.now();
        List<StaffLeave> activeLeavesToday = this.staffLeaveRepository.findActiveLeavesAt(today);

        List<StaffPerformance> staffList = basicMetrics.stream()
                .map(metric -> {
                    staffProfiles.stream()
                            .filter(profile -> profile.getId().equals(metric.getStaffId()))
                            .findFirst()
                            .ifPresent(profile -> {
                                // Staff Code & Role
                                String staffCode = (profile.getUser() != null && profile.getUser().getCode() != null)
                                        ? profile.getUser().getCode()
                                        : "St-00" + profile.getId();
                                metric.setStaffCode(staffCode);
                                metric.setStaffRole("Nail Artist");

                                // 🌟 User Entity ထဲမှ Profile Picture နှင့် အချက်အလက်များ Mapping ပြုလုပ်ခြင်း
                                if (profile.getUser() != null) {
                                    User user = profile.getUser();

                                    metric.setUserId(user.getId());
                                    metric.setStaffName(user.getFullName());
                                    metric.setPhoneNumber(user.getPhone());
                                    metric.setEmail(user.getEmail());

                                    // 🎯 Profile Picture
                                    String rawPhoto = user.getProfilePicture();
                                    String photoFileName = (rawPhoto != null && !rawPhoto.isBlank())
                                            ? rawPhoto
                                            : "default-profile.png";

                                    String relativePath = photoFileName.startsWith("/uploads/profile-pictures/")
                                            ? photoFileName
                                            : "/uploads/profile-pictures/" + photoFileName;

                                    metric.setProfileImage(relativePath);

                                    // 🎯 Date of Birth Mapping
                                    if (user.getDateOfBirth() != null) {
                                        LocalDate dob = user.getDateOfBirth().atZone(yangonZone).toLocalDate();
                                        metric.setDateOfBirth(dob);
                                    } else {
                                        metric.setDateOfBirth(null);
                                    }

                                    // 🎯 Joined Date Mapping
                                    if (user.getCreatedAt() != null) {
                                        LocalDate joinedDate = user.getCreatedAt().atZone(yangonZone).toLocalDate();
                                        metric.setJoinedDate(joinedDate);
                                    } else {
                                        metric.setDateOfBirth(null);
                                    }
                                } else {
                                    metric.setProfileImage("/uploads/profile-pictures/default-profile.png");
                                }

                                // 🌟 [FIXED] Status Mapping Logic (Day Off / Leave စစ်ဆေးချက် ထည့်သွင်းထားသည်)
                                boolean isAccountActive = profile.getUser() != null && profile.getUser().isEnabled();
                                boolean isStaffAvailable = profile.isAvailable();
                                boolean hasActiveJob = profile.getAssignedBookings() != null && profile.getAssignedBookings().stream()
                                        .anyMatch(b -> b.getStatus() == BookingStatus.IN_PROGRESS);

                                // ယနေ့ ခွင့် / Day Off ယူထားခြင်း ရှိမရှိ စစ်ဆေးခြင်း
                                boolean isOnLeaveToday = activeLeavesToday != null && activeLeavesToday.stream()
                                        .anyMatch(l -> l.getStaffProfile() != null && l.getStaffProfile().getId().equals(profile.getId()));

                                if (!isAccountActive) {
                                    metric.setStatus("Inactive");
                                } else if (isOnLeaveToday) {
                                    // 🎯 Day Off သို့မဟုတ် Leave ယူထားပါက In Progress ထဲ မပါစေဘဲ Unavailable အဖြစ် သတ်မှတ်မည်
                                    metric.setStatus("Unavailable");
                                } else if (hasActiveJob) {
                                    metric.setStatus("In Progress");
                                } else if (!isStaffAvailable) {
                                    metric.setStatus("Unavailable");
                                } else {
                                    metric.setStatus("Available");
                                }

                                // Specialized Services IDs Mapping
                                if (profile.getSpecializedServices() != null) {
                                    metric.setSpecializedServiceIds(
                                            profile.getSpecializedServices().stream()
                                                    .map(BusinessService::getId)
                                                    .toList()
                                    );
                                }

                                // ရွေးချယ်ထားသော လအလိုက် Completed Bookings filter ပြုလုပ်ခြင်း
                                if (profile.getAssignedBookings() != null) {
                                    List<Booking> completedBookings = profile.getAssignedBookings().stream()
                                            .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
                                            .filter(b -> b.getBookingDate() != null)
                                            .filter(b -> {
                                                LocalDate bDate = b.getBookingDate().atZone(yangonZone).toLocalDate();
                                                return bDate.getMonthValue() == targetDate.getMonthValue()
                                                        && bDate.getYear() == targetDate.getYear();
                                            })
                                            .toList();

                                    // Completed Jobs Count တွက်ချက်ခြင်း
                                    metric.setCompletedJobsCount((long) completedBookings.size());

                                    // Revenue & Commission တွက်ချက်ခြင်း
                                    double revenue = completedBookings.stream()
                                            .filter(b -> b.getPayment() != null && b.getPayment().getAmount() != null)
                                            .mapToDouble(b -> b.getPayment().getAmount().doubleValue())
                                            .sum();

                                    metric.setTotalRevenue(revenue);
                                    metric.setTotalCommission(revenue * 0.20);
                                }
                            });
                    return metric;
                })
                .sorted((s1, s2) -> Double.compare(s2.getRatingAverage(), s1.getRatingAverage()))
                .toList();

        // Counts တွက်ချက်ခြင်း
        long availableCount = staffList.stream().filter(s -> "Available".equalsIgnoreCase(s.getStatus())).count();
        long inProgressCount = staffList.stream().filter(s -> "In Progress".equalsIgnoreCase(s.getStatus())).count();
        long unavailableCount = staffList.stream().filter(s -> "Unavailable".equalsIgnoreCase(s.getStatus())).count();
        long inactiveCount = staffList.stream().filter(s -> "Inactive".equalsIgnoreCase(s.getStatus())).count();

        return StaffOverviewWrapper.builder()
                .availableCount(availableCount)
                .inProgressCount(inProgressCount)
                .unavailableCount(unavailableCount)
                .inactiveCount(inactiveCount)
                .staffList(staffList)
                .build();
    }

    @Override
    public DashboardStatsResponse getDashboardStats(Integer month, Integer year) {
        LocalDate targetDate = resolveTargetDate(month, year);
        LocalDate lastMonthDate = targetDate.minusMonths(1); // ပြီးခဲ့သည့်လအား Dynamic တွက်ချက်ခြင်း

        int todayActiveStaff = this.staffProfileRepository.countByIsAvailableTrue();

        // ==================== 💾 OPTIMIZED DB FETCH ====================
        List<Booking> currentMonthBookings = this.bookingRepository.findAllByMonthAndYear(
                targetDate.getMonthValue(),
                targetDate.getYear()
        );

        List<Booking> lastMonthBookings = this.bookingRepository.findAllByMonthAndYear(
                lastMonthDate.getMonthValue(),
                lastMonthDate.getYear()
        );

        // ==================== 📊 Target Month Stats ====================
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

        // ==================== 📉 Last Month Stats ====================
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
    }

    @Override
    public List<ChartDataPoint> getChartData(String period) {
        LocalDate today = LocalDate.now(yangonZone);
        List<ChartDataPoint> chartData = new ArrayList<>();

        if ("monthly".equalsIgnoreCase(period)) {
            int currentYear = today.getYear();

            LocalDateTime startOfYear = LocalDate.of(currentYear, 1, 1).atStartOfDay();
            LocalDateTime endOfYear = LocalDate.of(currentYear, 12, 31).atTime(LocalTime.MAX);

            Instant startInstant = startOfYear.atZone(yangonZone).toInstant();
            Instant endInstant = endOfYear.atZone(yangonZone).toInstant();

            List<Booking> bookings = this.bookingRepository.findByBookingDateBetween(startInstant, endInstant);

            String[] months = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
            for (String monthLabel : months) {
                chartData.add(ChartDataPoint.builder()
                        .label(monthLabel)
                        .bookingCount(0)
                        .walkInCount(0)
                        .cancelCount(0)
                        .totalBooking(0)
                        .build());
            }

            for (Booking b : bookings) {
                if (b.getBookingDate() == null) continue;
                int monthValue = b.getBookingDate().atZone(yangonZone).getMonthValue();
                ChartDataPoint dataPoint = chartData.get(monthValue - 1);
                populateCounts(b, dataPoint);
            }

        } else {
            LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            LocalDate sunday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

            LocalDateTime startOfWeek = monday.atStartOfDay();
            LocalDateTime endOfWeek = sunday.atTime(LocalTime.MAX);

            Instant startInstant = startOfWeek.atZone(yangonZone).toInstant();
            Instant endInstant = endOfWeek.atZone(yangonZone).toInstant();

            List<Booking> bookings = this.bookingRepository.findByBookingDateBetween(startInstant, endInstant);

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
        dataPoint.setTotalBooking(dataPoint.getBookingCount() + dataPoint.getWalkInCount());
    }

    @Override
    public ReportSummaryResponse getReportChartData(Integer year, Integer month, String period) {
        List<Booking> bookings;
        Instant startInstant;
        Instant endInstant;

        int selectedYear = (year != null) ? year : LocalDate.now(yangonZone).getYear();
        int selectedMonth = (month != null) ? month : LocalDate.now(yangonZone).getMonthValue();

        if ("monthly".equalsIgnoreCase(period)) {
            LocalDateTime startOfYear = LocalDate.of(selectedYear, 1, 1).atStartOfDay();
            LocalDateTime endOfYear = LocalDate.of(selectedYear, 12, 31).atTime(LocalTime.MAX);
            startInstant = startOfYear.atZone(yangonZone).toInstant();
            endInstant = endOfYear.atZone(yangonZone).toInstant();
        } else {
            LocalDate startDay = LocalDate.of(selectedYear, selectedMonth, 1);
            LocalDateTime startOfMonth = startDay.atStartOfDay();
            LocalDateTime endOfMonth = startDay.with(TemporalAdjusters.lastDayOfMonth()).atTime(LocalTime.MAX);
            startInstant = startOfMonth.atZone(yangonZone).toInstant();
            endInstant = endOfMonth.atZone(yangonZone).toInstant();
        }

        bookings = this.bookingRepository.findByBookingDateBetween(startInstant, endInstant);

        Map<BusinessService, Long> serviceCountMap = bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED && b.getBusinessService() != null)
                .collect(Collectors.groupingBy(Booking::getBusinessService, Collectors.counting()));

        BusinessService topTrendingService = serviceCountMap.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        String topServiceName = (topTrendingService != null) ? topTrendingService.getName() : "No Service";

        Map<String, ReportChartDataPoint> dataMap = new LinkedHashMap<>();
        if ("monthly".equalsIgnoreCase(period)) {
            String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
            for (String m : months) {
                dataMap.put(m, createEmptyDataPoint(m, topServiceName));
            }
        } else {
            for (DayOfWeek day : DayOfWeek.values()) {
                String shortName = day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
                dataMap.put(shortName, createEmptyDataPoint(shortName, topServiceName));
            }
        }

        for (Booking b : bookings) {
            if (b.getBookingDate() == null) continue;

            String key;
            if ("monthly".equalsIgnoreCase(period)) {
                int monthValue = b.getBookingDate().atZone(yangonZone).getMonthValue();
                String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
                key = months[monthValue - 1];
            } else {
                DayOfWeek dayOfWeek = b.getBookingDate().atZone(yangonZone).getDayOfWeek();
                key = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            }

            ReportChartDataPoint dataPoint = dataMap.get(key);
            if (dataPoint == null) continue;

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

            if (b.getStatus() == BookingStatus.COMPLETED) {
                java.math.BigDecimal paymentBigDecimal = (b.getPayment() != null) ? b.getPayment().getAmount() : java.math.BigDecimal.ZERO;
                double paymentAmount = paymentBigDecimal.doubleValue();

                RevenueBlock revBlock = dataPoint.getRevenueBlock();
                revBlock.setTotalRevenue(revBlock.getTotalRevenue() + paymentAmount);

                if (topTrendingService != null && topTrendingService.getId().equals(b.getBusinessService().getId())) {
                    revBlock.setTopServiceRevenue(revBlock.getTopServiceRevenue() + paymentAmount);
                }
            }
        }

        double grandTotalRevenue = dataMap.values().stream()
                .mapToDouble(dp -> dp.getRevenueBlock().getTotalRevenue())
                .sum();

        double grandTopServiceRevenue = dataMap.values().stream()
                .mapToDouble(dp -> dp.getRevenueBlock().getTopServiceRevenue())
                .sum();

        return ReportSummaryResponse.builder()
                .grandRevenue(RevenueBlock.builder()
                        .totalRevenue(grandTotalRevenue)
                        .topServiceRevenue(grandTopServiceRevenue)
                        .build())
                .chartData(new ArrayList<>(dataMap.values()))
                .build();
    }

    private ReportChartDataPoint createEmptyDataPoint(String label, String topServiceName) {
        return ReportChartDataPoint.builder()
                .label(label)
                .bookingCount(0)
                .walkInCount(0)
                .cancelCount(0)
                .totalBooking(0)
                .revenueBlock(RevenueBlock.builder().totalRevenue(0.0).topServiceRevenue(0.0).build())
                .topServiceName(topServiceName)
                .build();
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
    public PaginatedDailyOverviewResponse getMonthlyDailyOverview(Integer year, Integer month, int page, int size) {
        int selectedYear = (year != null) ? year : LocalDate.now(yangonZone).getYear();
        int selectedMonth = (month != null) ? month : LocalDate.now(yangonZone).getMonthValue();

        LocalDate startDayOfMonth = LocalDate.of(selectedYear, selectedMonth, 1);
        LocalDate endDayOfMonth = startDayOfMonth.with(TemporalAdjusters.lastDayOfMonth());

        LocalDateTime startOfMonth = startDayOfMonth.atStartOfDay();
        LocalDateTime endOfMonth = endDayOfMonth.atTime(LocalTime.MAX);

        Instant startInstant = startOfMonth.atZone(yangonZone).toInstant();
        Instant endInstant = endOfMonth.atZone(yangonZone).toInstant();

        List<Booking> monthBookings = this.bookingRepository.findByBookingDateBetween(startInstant, endInstant);

        Map<Integer, List<Booking>> bookingsByDayMap = monthBookings.stream()
                .filter(b -> b.getBookingDate() != null)
                .collect(Collectors.groupingBy(b -> b.getBookingDate().atZone(yangonZone).getDayOfMonth()));

        long totalStaffCount = this.staffProfileRepository.count();
// 🎯 LocalDate အစား အပေါ်မှာ တွက်ထားပြီးသား Instant ဒေတာများကို ပြောင်းလဲပေးလိုက်ပါတယ်
        List<StaffLeave> monthlyLeaves = this.staffLeaveRepository.findLeavesInPeriod(startInstant, endInstant);

        int totalDaysInMonth = startDayOfMonth.lengthOfMonth();
        int totalPages = (int) Math.ceil((double) totalDaysInMonth / size);

        int startDay = (page * size) + 1;
        int endDay = Math.min(startDay + size - 1, totalDaysInMonth);

        List<DailyOverviewDataPoint> pagedData = new ArrayList<>();

        for (int d = startDay; d <= endDay; d++) {
            LocalDate currentDate = LocalDate.of(selectedYear, selectedMonth, d);
            List<Booking> dayBookings = bookingsByDayMap.getOrDefault(d, new ArrayList<>());

            long totalBookings = dayBookings.size();
            long cancelledBookings = dayBookings.stream()
                    .filter(b -> b.getStatus() == BookingStatus.CANCELLED)
                    .count();
            long walkInCustomers = dayBookings.stream()
                    .filter(b -> b.getStatus() != BookingStatus.CANCELLED && b.getCustomer() != null && "CU-WALKIN".equalsIgnoreCase(b.getCustomer().getCode()))
                    .count();

            // ==================== 🌟 ACTIVE STAFF DETERMINATION LOGIC (UPDATED WITH INSTANT FIX) ====================
            long staffOnLeaveCount = monthlyLeaves.stream()
                    .filter(leave -> {
                        LocalDate leaveStart = leave.getStartDate().atZone(yangonZone).toLocalDate();
                        LocalDate leaveEnd = leave.getEndDate().atZone(yangonZone).toLocalDate();
                        return !currentDate.isBefore(leaveStart) && !currentDate.isAfter(leaveEnd);
                    })
                    .map(leave -> leave.getStaffProfile().getId())
                    .distinct()
                    .count();

            long activeStaff = totalStaffCount - staffOnLeaveCount;
            if (activeStaff < 0) activeStaff = 0;
            // ===================================================================================================

            double totalRevenue = dayBookings.stream()
                    .filter(b -> b.getStatus() == BookingStatus.COMPLETED && b.getPayment() != null)
                    .mapToDouble(b -> b.getPayment().getAmount().doubleValue())
                    .sum();

            String topService = dayBookings.stream()
                    .filter(b -> b.getStatus() == BookingStatus.COMPLETED && b.getBusinessService() != null)
                    .collect(Collectors.groupingBy(b -> b.getBusinessService().getName(), Collectors.counting()))
                    .entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("-");

            String dateLabel = d + "." + selectedMonth + "." + selectedYear;

            pagedData.add(DailyOverviewDataPoint.builder()
                    .date(dateLabel)
                    .totalBookings(totalBookings)
                    .cancelledBookings(cancelledBookings)
                    .walkInCustomers(walkInCustomers)
                    .activeStaff(activeStaff)
                    .totalRevenue(totalRevenue)
                    .topService(topService)
                    .build());
        }

        return PaginatedDailyOverviewResponse.builder()
                .content(pagedData)
                .pageNumber(page)
                .pageSize(size)
                .totalPages(totalPages)
                .totalElements(totalDaysInMonth)
                .build();
    }

    @Override
    public StaffPerformance getStaffPerformanceById(Long staffId) {
        LocalDate today = LocalDate.now(yangonZone);

        return this.getStaffPerformanceRanking(today.getMonthValue(), today.getYear())
                .getStaffList() // 🌟 Wrapper ထဲမှ staffList (List<StaffPerformance>) ကို အရင်ထုတ်ယူရပါမည်
                .stream()
                .filter(metric -> metric.getStaffId() != null && metric.getStaffId().equals(staffId))
                .findFirst()
                .orElseThrow(() -> new com.codingproject.digitalbase.exception.ResourceNotFoundException("Staff not found: " + staffId));
    }

    private double calculateGrowth(double current, double previous) {
        if (previous == 0) return current > 0 ? 100.0 : 0.0;
        double growth = ((current - previous) / previous) * 100.0;
        return Math.round(growth * 100.0) / 100.0;
    }

    private boolean isWalkInBooking(Booking b) {
        return b.getCustomer() != null && "CU-WALKIN".equals(b.getCustomer().getCode());
    }
}