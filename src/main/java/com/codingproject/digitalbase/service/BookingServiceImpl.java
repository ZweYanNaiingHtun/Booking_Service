//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.codingproject.digitalbase.service;

import com.codingproject.digitalbase.dtos.*;
import com.codingproject.digitalbase.enums.*;
import com.codingproject.digitalbase.exception.BadRequestException;
import com.codingproject.digitalbase.exception.ResourceNotFoundException;
import com.codingproject.digitalbase.exception.UnauthorizedException;
import com.codingproject.digitalbase.model.*;
import com.codingproject.digitalbase.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final BusinessServiceRepository serviceRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final HttpServletRequest httpServletRequest;
    private final StaffProfileRepository staffProfileRepository;
    private final StaffAssignmentRepository staffAssignmentRepository;
    private final FCMService fcmService;
    private final PaymentRepository paymentRepository;
    private final AuthService authService;
    private final NotificationRepository notificationRepository;

    private List<Booking> getOverlappingPendingBookings(Instant bookingDate, Instant staffStartTime, Instant staffEndTime, int bufferMinutes) {
        // 🌟 ၁။ ၎င်းနေ့ရက်၏ တစ်ရက်တာအတွင်းရှိသော ဒေတာများကိုသာ DB မှ ဆွဲထုတ်ရန် Boundary သတ်မှတ်ခြင်း
        ZoneId zoneId = ZoneId.of("Asia/Yangon");
        ZonedDateTime zonedDateTime = bookingDate.atZone(zoneId);

        Instant startOfDay = zonedDateTime.toLocalDate().atStartOfDay(zoneId).toInstant();
        Instant endOfDay = zonedDateTime.toLocalDate().plusDays(1).atStartOfDay(zoneId).toInstant();

        // ၎င်းနေ့ရက်၏ PENDING ဘွတ်ကင်များအားလုံးကို ဆွဲထုတ်ခြင်း
        List<Booking> dailyPendingBookings = bookingRepository.findByStatusAndBookingDateBetween(BookingStatus.PENDING, startOfDay, endOfDay);

        // 🌟 ၂။ Instant Math ဖြင့် ပိုမိုသန့်ရှင်းမြန်ဆန်စွာ အချိန်ထပ်နေခြင်း (Overlap) ကို စစ်ထုတ်ခြင်း
        return dailyPendingBookings.stream().filter(pb -> {
            Instant pbBookingDate = pb.getBookingDate();
            Instant pbStaffStart = pbBookingDate.minus(Duration.ofMinutes(bufferMinutes));

            int pbDuration = pb.getBusinessService().getDurationInMinutes();
            Instant pbStaffEnd = pbBookingDate.plus(Duration.ofMinutes(pbDuration + bufferMinutes));

            // Standard Interval Overlap Formula (A_Start < B_End && A_End > B_Start)
            return staffStartTime.isBefore(pbStaffEnd) && staffEndTime.isAfter(pbStaffStart);
        }).toList();
    }

    @Override
    public List<StaffTimeSlotResponse> getAvailableSlotsForStaffAndDate(Long staffUserId, Long serviceId, LocalDate date) {
        List<StaffTimeSlotResponse> timeSlots = new ArrayList<>();

        BusinessService service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));
        Integer duration = service.getDurationInMinutes();

        LocalTime openingTime = LocalTime.of(9, 0);
        LocalTime closingTime = LocalTime.of(20, 0); // 08:00 PM Close
        int slotIntervalMinutes = 60; // ၁ နာရီ Rigid Slot စနစ်
        int bufferMinutes = 10;

        ZoneId zoneId = ZoneId.of("Asia/Yangon");

        // =========================================================================
        // 🌟 [PERFORMANCE UPDATE] Loop အပြင်ဘက်တွင် တစ်နေ့တာလုံးစာ ဒေတာကို DB မှ တစ်ကြိမ်တည်း ကြိုတင်ဆွဲထုတ်ခြင်း
        // =========================================================================
        Instant startOfDay = date.atStartOfDay(zoneId).toInstant();
        Instant endOfDay = date.plusDays(1).atStartOfDay(zoneId).toInstant();

        // ဒီနေ့ရက်အတွက်ရှိသမျှ PENDING Bookings အားလုံးကို DB ကနေ ၁ ကြိမ်ပဲ ဆွဲယူပါတော့မယ်
        List<Booking> dailyPendingBookings = bookingRepository.findByStatusAndBookingDateBetween(
                BookingStatus.PENDING, startOfDay, endOfDay);
        // =========================================================================

        LocalTime currentSlotTime = openingTime;
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");

        while (currentSlotTime.isBefore(closingTime)) {
            LocalTime customerStartTime = currentSlotTime;
            LocalTime customerEndTime = customerStartTime.plusMinutes(duration);

            if (customerEndTime.isAfter(closingTime)) {
                currentSlotTime = currentSlotTime.plusMinutes(slotIntervalMinutes);
                continue;
            }

            Instant slotStartInstant = date.atTime(customerStartTime).atZone(zoneId).toInstant();
            Instant slotEndInstant = date.atTime(customerEndTime).atZone(zoneId).toInstant();

            Instant staffStartTimeBound = slotStartInstant.minus(Duration.ofMinutes(bufferMinutes));
            Instant staffEndTimeBound = slotEndInstant.plus(Duration.ofMinutes(bufferMinutes));

            // ၁။ Confirmed ဖြစ်ပြီးသား အလုပ်ရှိမရှိ စစ်ဆေးခြင်း
            boolean isStaffBusy = staffAssignmentRepository.isStaffBusy(staffUserId, staffStartTimeBound, staffEndTimeBound);

            // 🌟 ၂။ [In-Memory Overlap Check] DB သို့ မသွားတော့ဘဲ ကြိုဆွဲထားသော List ထဲမှ Java Memory ပေါ်တွင်တင် Filter စစ်ခြင်း
            boolean isRequestedByOtherPending = dailyPendingBookings.stream()
                    .filter(pb -> pb.getRequestedStaff() != null && pb.getRequestedStaff().getUser().getId().equals(staffUserId))
                    .anyMatch(pb -> {
                        Instant pbBookingDate = pb.getBookingDate();
                        Instant pbStaffStart = pbBookingDate.minus(Duration.ofMinutes(bufferMinutes));

                        int pbDuration = pb.getBusinessService().getDurationInMinutes();
                        Instant pbStaffEnd = pbBookingDate.plus(Duration.ofMinutes(pbDuration + bufferMinutes));

                        // Interval Overlap Formula: (StartA < EndB && EndA > StartB)
                        return staffStartTimeBound.isBefore(pbStaffEnd) && staffEndTimeBound.isAfter(pbStaffStart);
                    });

            boolean isAvailable = !isStaffBusy && !isRequestedByOtherPending;

            timeSlots.add(StaffTimeSlotResponse.builder()
                    .timeLabel(customerStartTime.format(timeFormatter))
                    .bookingDate(slotStartInstant)
                    .isAvailable(isAvailable)
                    .build());

            currentSlotTime = currentSlotTime.plusMinutes(slotIntervalMinutes);
        }

        return timeSlots;
    }

    @Override
    @Transactional
    public BookingResponse createCustomerBooking(BookingRequest request) {
        User currentUser = getCurrentAuthenticatedUser();

        log.info("============= [DEBUG] =============");
        log.info("→ Current Authenticated User ID: {}", currentUser.getId());
        log.info("→ Requested Booking Date: {}", request.getBookingDate());
        log.info("===================================");

        BusinessService service = serviceRepository.findById(request.getServiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));

        Integer duration = service.getDurationInMinutes();
        Instant customerBookingInstant = request.getBookingDate();

        ZoneId zoneId = ZoneId.of("Asia/Yangon");
        ZonedDateTime zonedDateTime = customerBookingInstant.atZone(zoneId);
        LocalTime customerStartTime = zonedDateTime.toLocalTime();

        if (zonedDateTime.getMinute() != 0 || zonedDateTime.getSecond() != 0) {
            throw new BadRequestException("Booking Denied! Invalid time selection. Please choose an exact rigid hour slot from the calendar (e.g., 09:00 AM, 10:00 AM).");
        }

        LocalTime openingTime = LocalTime.of(9, 0);
        LocalTime closingTime = LocalTime.of(20, 0);
        LocalTime customerEndTime = customerStartTime.plusMinutes(duration);

        if (customerStartTime.isBefore(openingTime)) {
            throw new BadRequestException(String.format("Booking Denied! Booking time [%s] is invalid. Our salon operating hours are from 09:00 AM to 08:00 PM.",
                    customerStartTime.toString()));
        }

        if (customerEndTime.isAfter(closingTime)) {
            throw new BadRequestException(String.format("Booking Denied! The '%s' service takes %d minutes and will end at [%s], which exceeds our closing time (08:00 PM). Please select an earlier time slot.",
                    service.getName(), duration, customerEndTime.toString()));
        }

        List<BookingStatus> activeStatuses = List.of(
                BookingStatus.PENDING,
                BookingStatus.CONFIRMED,
                BookingStatus.IN_PROGRESS
        );

        boolean hasExistingBooking = bookingRepository.existsByCustomerIdAndBookingDateAndStatusIn(
                currentUser.getId(),
                customerBookingInstant,
                activeStatuses
        );

        if (hasExistingBooking) {
            throw new BadRequestException("Booking Denied! You already have another active booking (Pending/Confirmed/In Progress) at this exact time slot.");
        }

        int bufferMinutes = 10;
        Instant staffStartTime = customerBookingInstant.minus(Duration.ofMinutes(bufferMinutes));
        Instant staffEndTime = customerBookingInstant.plus(Duration.ofMinutes(duration + bufferMinutes));

        List<StaffProfile> activeStaffProfiles = staffProfileRepository.findByIsAvailableTrue();
        List<Booking> overlappingPendingBookings = getOverlappingPendingBookings(customerBookingInstant, staffStartTime, staffEndTime, bufferMinutes);

        long confirmedBusyCount = activeStaffProfiles.stream()
                .filter(sp -> staffAssignmentRepository.isStaffBusy(sp.getUser().getId(), staffStartTime, staffEndTime))
                .count();
        int pendingCount = overlappingPendingBookings.size();

        if (confirmedBusyCount + pendingCount >= activeStaffProfiles.size()) {
            throw new BadRequestException("Sorry, no staff members are available at this specific date and time! All salon capacities are full.");
        }

        StaffProfile requestedStaffProfile = null;

        if (request.getRequestedStaffId() != null) {
            requestedStaffProfile = staffProfileRepository.findByUserId(request.getRequestedStaffId())
                    .orElseThrow(() -> new ResourceNotFoundException("Requested staff profile not found"));

            if (isStaffOnLeave(requestedStaffProfile, customerBookingInstant)) {
                throw new BadRequestException("Booking Denied! The requested staff member is on leave or day-off on this specific date.");
            }

            boolean isBusy = staffAssignmentRepository.isStaffBusy(requestedStaffProfile.getUser().getId(), staffStartTime, staffEndTime);
            if (isBusy) {
                throw new BadRequestException("The requested staff is busy or in preparation/cleanup time for another booking. Please choose another time slot!");
            }

            final Long reqStaffId = requestedStaffProfile.getId();
            boolean isRequestedByOtherPending = overlappingPendingBookings.stream()
                    .anyMatch(pb -> pb.getRequestedStaff() != null && pb.getRequestedStaff().getId().equals(reqStaffId));

            if (isRequestedByOtherPending) {
                throw new BadRequestException("The requested staff has another pending request for this time slot. Please choose another staff member or time!");
            }
        }

        Booking booking = new Booking();
        booking.setCustomer(currentUser);
        booking.setCreatedBy(currentUser);
        booking.setBusinessService(service);
        booking.setBookingDate(customerBookingInstant);
        booking.setNotes(request.getNotes());
        booking.setStatus(BookingStatus.PENDING);
        booking.setCreatedAt(Instant.now());
        booking.setRequestedStaff(requestedStaffProfile);

        Booking savedBooking = bookingRepository.save(booking);

        // =========================================================================
        // 1. DB NOTIFICATION FOR ADMIN INBOX (CUSTOMER PANEL -> ORDERED FILTER)
        // =========================================================================
        String serviceName = service.getName();
        String customerName = currentUser.getFullName();
        String adminNotiTitle = "New Booking Ordered 📩";
        String adminNotiBody = customerName + " has placed a new booking for " + serviceName + ".";

        java.util.Map<String, Object> adminMetadata = new java.util.HashMap<>();
        adminMetadata.put("bookingId", savedBooking.getId().toString());
        adminMetadata.put("serviceName", serviceName);
        adminMetadata.put("bookingStatus", "PENDING");
        adminMetadata.put("customerName", customerName);
        adminMetadata.put("bookingDate", savedBooking.getBookingDate().toString());

        Notification adminOrderNotification = Notification.builder()
                .title(adminNotiTitle)
                .message(adminNotiBody)
                .type(NotificationType.BOOKING)
                .targetAudience(TargetAudience.CUSTOMER)
                .customerAction(CustomerAction.ORDERED)
                .bookingStatus(BookingStatus.PENDING)
                .user(currentUser)
                .metadata(adminMetadata)
                .isRead(false)
                .createdAt(Instant.now())
                .build();
        this.notificationRepository.save(adminOrderNotification);
        log.info("DB Notification saved for Admin Inbox (Customer -> Ordered).");

        // =========================================================================
        // 2. REAL-TIME DASHBOARD TOPIC PUSH (STRICTLY ENGLISH)
        // =========================================================================
        try {
            String testTopic = "booking-updates";
            String title = "New Booking Alert! 🎉";
            String body = customerName + " has placed a new booking for " + serviceName + ".";
            fcmService.sendPushNotificationToTopic(testTopic, title, body);
            log.info("Real-time UI Update Push sent to Dashboard Topic successfully.");
        } catch (Exception e) {
            log.error("Topic Notification bypassed: {}", e.getMessage());
        }

        return mapToResponse(savedBooking);
    }

    @Override
    @Transactional
    public BookingResponse createWalkInBooking(WalkInBookingRequest request) {
        try {
            User currentStaff = getCurrentAuthenticatedUser();

            StaffProfile staffProfile = currentStaff.getStaffProfile();
            if (staffProfile == null) {
                throw new ResourceNotFoundException("လက်ရှိ Login ဝင်ထားသော ဝန်ထမ်းတွင် Staff Profile ဆောက်ထားခြင်း မရှိသေးပါ!");
            }

            BusinessService service = serviceRepository.findById(request.getServiceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Service not found"));

            Role customerRole = roleRepository.findByRole(RoleName.CUSTOMER)
                    .orElseThrow(() -> new ResourceNotFoundException("Default CUSTOMER role not found!"));

            User walkInCustomer = userRepository.findByPhone("000000000")
                    .orElseGet(() -> userRepository.save(User.builder()
                            .fullName("Walk-In Customer")
                            .phone("000000000")
                            .code("CU-WALKIN")
                            .email("walkin@system.com")
                            .password(passwordEncoder.encode("SystemWalkIn@123"))
                            .gender("UNKNOWN")
                            .enabled(true)
                            .createdAt(Instant.now())
                            .roles(new HashSet<>(Collections.singleton(customerRole)))
                            .build()));

            Instant bookingInstant = request.getBookingDate() != null ? request.getBookingDate() : Instant.now();
            Instant endInstant = bookingInstant.plus(Duration.ofHours(1));

            StaffAssignment assignment = StaffAssignment.builder()
                    .staffProfile(staffProfile)
                    .startTime(bookingInstant)
                    .endTime(endInstant)
                    .isBooked(true)
                    .build();

            StaffAssignment savedAssignment = staffAssignmentRepository.save(assignment);

            Booking booking = new Booking();
            booking.setCustomer(walkInCustomer);
            booking.setCreatedBy(currentStaff);
            booking.setAssignedStaff(staffProfile);
            booking.setStaffAssignment(savedAssignment);
            booking.setBusinessService(service);
            booking.setBookingDate(bookingInstant);
            booking.setNotes(request.getNotes());
            booking.setStatus(BookingStatus.COMPLETED);
            booking.setCreatedAt(Instant.now());

            Booking savedBooking = bookingRepository.save(booking);

            BigDecimal baseAmount = request.getAmount() != null ? request.getAmount() : service.getPrice();
            BigDecimal extraAmount = request.getExtraAmount() != null ? request.getExtraAmount() : BigDecimal.ZERO;
            BigDecimal totalAmount = baseAmount.add(extraAmount);

            Payment payment = Payment.builder()
                    .booking(savedBooking)
                    .baseAmount(baseAmount)
                    .extraAmount(extraAmount)
                    .amount(totalAmount)
                    .paymentMethod(request.getPaymentType())
                    .status("SUCCESS")
                    .createdAt(Instant.now())
                    .build();

            paymentRepository.save(payment);

            // =========================================================================
            // 🌟 1. DB NOTIFICATION FOR ADMIN INBOX (STAFF PANEL -> COMPLETED FILTER)
            // =========================================================================
            String serviceName = service.getName();
            String staffName = currentStaff.getFullName();
            String adminNotiTitle = "Walk-In Booking Completed ✅";
            String adminNotiBody = staffName + " has processed a walk-in booking for " + serviceName + ".";

            java.util.Map<String, Object> adminMetadata = new java.util.HashMap<>();
            adminMetadata.put("bookingId", savedBooking.getId().toString());
            adminMetadata.put("serviceName", serviceName);
            adminMetadata.put("bookingStatus", "COMPLETED");
            adminMetadata.put("staffName", staffName);
            adminMetadata.put("totalAmount", totalAmount.toString());

            Notification adminWalkInNotification = Notification.builder()
                    .title(adminNotiTitle)
                    .message(adminNotiBody)
                    .type(NotificationType.BOOKING)
                    .targetAudience(TargetAudience.STAFF)      // Admin Inbox ၏ Staff Pane သို့ စီးဆင်းရန်
                    .bookingStatus(BookingStatus.COMPLETED)    // tab=completed Filter နှင့် ချိတ်ဆက်ရန်
                    .user(currentStaff)
                    .metadata(adminMetadata)
                    .isRead(false)
                    .createdAt(Instant.now())
                    .build();
            this.notificationRepository.save(adminWalkInNotification);
            log.info("✅ DB Notification saved for Admin Inbox (Staff -> Completed).");

            // =========================================================================
            // 🌟 2. TOPIC FCM PUSH FOR ADMIN DASHBOARD REAL-TIME UPDATE
            // =========================================================================
            try {
                String testTopic = "booking-updates";
                String title = "Walk-In Booking Alert! 🛒";
                String body = staffName + " က Walk-In Customer အတွက် " + serviceName + " (Completed) ကို စာရင်းသွင်းလိုက်ပါပြီ။";
                fcmService.sendPushNotificationToTopic(testTopic, title, body);
                log.info("✅ FCM Topic Push triggered for Walk-In booking.");
            } catch (Exception e) {
                log.error("⚠️ Topic Notification bypassed for Walk-In: {}", e.getMessage());
            }

            return mapToResponse(savedBooking);

        } catch (Exception e) {
            System.out.println("============== 🚨 WALK-IN ERROR FOUND 🚨 ==============");
            e.printStackTrace();
            System.out.println("=======================================================");
            throw e;
        }
    }

    public Page<BookingHistoryResponse> getMyBookingHistory(int page, int size) {
        User currentUser = this.getCurrentAuthenticatedUser();

        // CreatedAt အရ နောက်ဆုံးတင်ထားသော Booking ကို အပေါ်ဆုံးကပြရန် Descending သုံးထားပါသည်
        Pageable pageable = PageRequest.of(page, size, Sort.by(new String[]{"createdAt"}).descending());

        Page<Booking> bookingPage = this.bookingRepository.findByCustomerId(currentUser.getId(), pageable);

        return bookingPage.map(booking -> {

            // 🎯 ၁။ Fix: getService() အစား getBusinessService() ကို သုံးထားပါသည်
            String mainServiceName = (booking.getBusinessService() != null) ? booking.getBusinessService().getName() : "Nail Service";

            // 💡 မှတ်ချက် - BusinessService ထဲတွင် duration field ရှိပါက ယူရန် (မရှိပါက default 50 ဟု UI အတိုင်း ထည့်ထားနိုင်ပါသည်)
            int duration = booking.getBusinessService().getDurationInMinutes();

            // 🎯 ၂။ Fix: getStaffProfile() အစား ဆရာကြီးရဲ့ မူရင်း getAssignedStaff() ကို ပြောင်းလဲထားပါသည်
            String artistName = "Assigning...";
            if (booking.getRequestedStaff() != null && booking.getRequestedStaff().getUser() != null) {
                artistName = booking.getRequestedStaff().getUser().getFullName();
            }

            // 🎯 ၃။ Fix: UI ထဲက "BK-12345" ပုံစံရရန် booking.getId() ကို သုံးထားပါသည်
            String bookingCode = "BK-" + booking.getId();

            // 🎯 ၄။ Fix: စျေးနှုန်းကို getPayment() သို့မဟုတ် BusinessService Price မှ ယူခြင်း
            BigDecimal price = BigDecimal.ZERO;
            if (booking.getPayment() != null && booking.getPayment().getAmount() != null) {
                price = booking.getPayment().getAmount();
            } else if (booking.getBusinessService() != null && booking.getBusinessService().getPrice() != null) {
                price = booking.getBusinessService().getPrice();
            }

            return BookingHistoryResponse.builder()
                    .id(booking.getId())
                    .bookingCode(bookingCode)
                    .serviceName(mainServiceName)
                    .totalDuration(duration)
                    .appointmentTime(booking.getBookingDate()) // 🎯 Fix: getAppointmentTime() အစား getBookingDate() (Instant) ကို သုံးပါသည်
                    .totalPrice(price)
                    .status(booking.getStatus().name())
                    .staffName(artistName)
                    .staffRole("Nail Artist")
                    .build();
        });
    }

    public Page<BookingResponse> getAllBookings(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(new String[]{"createdAt"}).descending());
        Page<Booking> bookingPage = this.bookingRepository.findAll(pageable);
        return bookingPage.map(this::mapToResponse);
    }

    @Transactional(
            readOnly = true
    )
    public BookingResponse getBookingById(Long id) {
        Booking booking = this.bookingRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));
        return this.mapToResponse(booking);
    }
    // 🌟 Dependency Inject လုပ်ထားရန်

    @Override
    @Transactional
    public BookingResponse confirmBooking(Long id) {
        log.info("--- Starting confirmBooking process for ID: {} ---", id);

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BadRequestException("Only PENDING bookings can be confirmed.");
        }

        // Buffer Time တွက်ချက်ခြင်း
        Instant customerStartTime = booking.getBookingDate();
        Instant staffStartTime = customerStartTime.minus(Duration.ofMinutes(10));
        Integer durationInMinutes = booking.getBusinessService().getDurationInMinutes();
        Instant staffEndTime = customerStartTime.plus(Duration.ofMinutes(durationInMinutes + 20));

        StaffProfile finalStaffProfile = null;

        // Staff ခွဲဝေမှု Logic
        if (booking.getRequestedStaff() != null) {
            finalStaffProfile = booking.getRequestedStaff();
            boolean isBusy = staffAssignmentRepository.isStaffBusy(finalStaffProfile.getUser().getId(), staffStartTime, staffEndTime);
            if (isBusy) {
                throw new BadRequestException("The requested staff is already busy during this confirmed slot. Admin, please re-assign manually!");
            }
        } else {
            List<StaffProfile> activeProfiles = staffProfileRepository.findByIsAvailableTrue();
            List<StaffProfile> availableStaffProfiles = activeProfiles.stream()
                    .filter(sp -> !staffAssignmentRepository.isStaffBusy(sp.getUser().getId(), staffStartTime, staffEndTime))
                    .toList();

            if (availableStaffProfiles.isEmpty()) {
                throw new BadRequestException("No staff members are available for this slot to auto-assign.");
            }

            List<StaffProfile> mutableList = new ArrayList<>(availableStaffProfiles);
            Collections.shuffle(mutableList);
            finalStaffProfile = mutableList.get(0);
        }

        // StaffAssignment တည်ဆောက်ခြင်း
        StaffAssignment assignment = StaffAssignment.builder()
                .staffProfile(finalStaffProfile)
                .startTime(staffStartTime)
                .endTime(staffEndTime)
                .isBooked(true)
                .booking(booking)
                .build();

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setAssignedStaff(finalStaffProfile);
        booking.setStaffAssignment(assignment);

        Booking savedBooking = bookingRepository.save(booking);
        log.info("Booking ID: {} status successfully updated to CONFIRMED.", id);

        String serviceName = savedBooking.getBusinessService().getName();
        String bookingDateStr = savedBooking.getBookingDate().toString();
        String assignedStaffName = finalStaffProfile.getUser().getFullName();

        // ==========================================
        // 🌟 1. CUSTOMER NOTIFICATION & DYNAMIC PUSH
        // ==========================================
        User customer = savedBooking.getCustomer();
        String customerNotiTitle = "Booking Confirmed! 🎉";
        String customerNotiBody = "Your booking for " + serviceName + " has been successfully confirmed with Staff: " + assignedStaffName;

        java.util.Map<String, Object> customerMetadata = new java.util.HashMap<>();
        customerMetadata.put("bookingId", savedBooking.getId().toString());
        customerMetadata.put("serviceName", serviceName);
        customerMetadata.put("bookingDate", bookingDateStr);
        customerMetadata.put("bookingStatus", "CONFIRMED");
        customerMetadata.put("staffName", assignedStaffName);

        Notification customerDbNotification = Notification.builder()
                .title(customerNotiTitle)
                .message(customerNotiBody)
                .type(NotificationType.BOOKING)
                .targetAudience(TargetAudience.CUSTOMER)
                .bookingStatus(BookingStatus.CONFIRMED)
                .user(customer)
                .metadata(customerMetadata)
                .isRead(false)
                .createdAt(Instant.now())
                .build();
        notificationRepository.save(customerDbNotification);

        if (customer.getFcmToken() != null && !customer.getFcmToken().isEmpty()) {
            try {
                fcmService.sendPushNotification(customer.getFcmToken(), customerNotiTitle, customerNotiBody);
                log.info("✅ FCM Push sent to Customer for confirmation.");
            } catch (Exception e) {
                log.error("❌ FCM Customer Push failed: {}", e.getMessage());
            }
        }

        // ==========================================
        // 🌟 2. STAFF NOTIFICATION & DYNAMIC PUSH
        // ==========================================
        User staffUser = finalStaffProfile.getUser();
        String staffNotiTitle = "New Booking Assigned! 📅";
        String staffNotiBody = "You have been assigned to a new booking: " + serviceName + " on " + bookingDateStr;

        java.util.Map<String, Object> staffMetadata = new java.util.HashMap<>();
        staffMetadata.put("bookingId", savedBooking.getId().toString());
        staffMetadata.put("serviceName", serviceName);
        staffMetadata.put("bookingDate", bookingDateStr);
        staffMetadata.put("bookingStatus", "CONFIRMED");
        staffMetadata.put("customerName", customer.getFullName());

        Notification staffDbNotification = Notification.builder()
                .title(staffNotiTitle)
                .message(staffNotiBody)
                .type(NotificationType.BOOKING)
                .targetAudience(TargetAudience.STAFF)
                .bookingStatus(BookingStatus.CONFIRMED)
                .user(staffUser)
                .metadata(staffMetadata)
                .isRead(false)
                .createdAt(Instant.now())
                .build();
        notificationRepository.save(staffDbNotification);
        log.info("Assigned Notification saved to DB for Staff: {}", staffUser.getEmail());

        if (staffUser.getFcmToken() != null && !staffUser.getFcmToken().isEmpty()) {
            try {
                fcmService.sendPushNotification(staffUser.getFcmToken(), staffNotiTitle, staffNotiBody);
                log.info("✅ FCM Push Notification sent to Assigned Staff successfully.");
            } catch (Exception e) {
                log.error("❌ FCM Staff Push failed: {}", e.getMessage());
            }
        }

        // ==========================================
        // 🌟 3. REAL-TIME TOPIC PUSH FOR ADMIN DASHBOARD
        // ==========================================
        try {
            fcmService.sendPushNotificationToTopic("booking-updates", "Booking Confirmed Alert!",
                    customer.getFullName() + " ၏ ဘွတ်ကင်ကို အတည်ပြုပြီး ဝန်ထမ်း " + assignedStaffName + " အား တာဝန်ပေးလိုက်ပါပြီ။");
        } catch (Exception e) {
            log.error("⚠️ Dashboard Topic Push bypassed: {}", e.getMessage());
        }

        BookingResponse response = mapToResponse(savedBooking);
        log.info("--- confirmBooking process successfully finished ---");
        return response;
    }

    @Override
    @Transactional
    public BookingResponse cancelBookingByAdmin(Long id, String cancelledBy, String userEmail, String reason) {
        log.info("--- Starting cancelBookingByAdmin process for ID: {} ---", id);

        Booking booking = this.bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));

        if ("CUSTOMER".equals(cancelledBy)) {
            if (!booking.getCustomer().getEmail().equals(userEmail)) {
                throw new BadRequestException("You are not authorized to cancel this booking.");
            }
            Instant now = Instant.now();
            Instant bookingTime = booking.getBookingDate();
            long hoursUntilBooking = Duration.between(now, bookingTime).toHours();
            if (hoursUntilBooking < 1L) {
                throw new BadRequestException("You can only cancel this booking at least 1 hour before the appointment.");
            }
        }

        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BadRequestException("Only PENDING or CONFIRMED bookings can be cancelled.");
        }

        StaffProfile staffToNotify = booking.getAssignedStaff() != null
                ? booking.getAssignedStaff()
                : booking.getRequestedStaff();

        if (booking.getStaffAssignment() != null) {
            this.staffAssignmentRepository.delete(booking.getStaffAssignment());
            booking.setStaffAssignment(null);
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setAssignedStaff(null);
        booking.setCancelledBy(cancelledBy);

        if ("ADMIN".equals(cancelledBy) && reason != null && !reason.isBlank()) {
            booking.setRejectionReason(reason);
        }

        Booking savedBooking = this.bookingRepository.save(booking);
        log.info("Booking ID: {} updated to CANCELLED.", id);

        String serviceName = savedBooking.getBusinessService().getName();
        String bookingDateStr = savedBooking.getBookingDate().toString();

        // ==========================================
        // 🌟 1. CUSTOMER DB NOTIFICATION & FCM PUSH (ADMIN INBOX MAPS HERE VIA CUSTOMER ACTION)
        // ==========================================
        User customer = savedBooking.getCustomer();
        String customerNotiTitle = "Booking Rejected ❌";
        String customerNotiBody = "Sorry, your booking for " + serviceName + " has been rejected by the admin.";
        if (reason != null && !reason.isBlank()) {
            customerNotiBody += " Reason: " + reason;
        }

        java.util.Map<String, Object> customerMetadata = new java.util.HashMap<>();
        customerMetadata.put("bookingId", savedBooking.getId().toString());
        customerMetadata.put("serviceName", serviceName);
        customerMetadata.put("bookingDate", bookingDateStr);
        customerMetadata.put("bookingStatus", "CANCELLED");
        customerMetadata.put("reason", reason != null ? reason : "");

        Notification customerDbNotification = Notification.builder()
                .title(customerNotiTitle)
                .message(customerNotiBody)
                .type(NotificationType.BOOKING)
                .targetAudience(TargetAudience.CUSTOMER)
                .customerAction(CustomerAction.CANCELLED) // 🎯 Admin Inbox (Customer -> Cancel Tab) သို့ စီးဆင်းစေရန်
                .bookingStatus(BookingStatus.CANCELLED)
                .user(customer)
                .metadata(customerMetadata)
                .isRead(false)
                .createdAt(Instant.now())
                .build();
        this.notificationRepository.save(customerDbNotification);

        if ("ADMIN".equals(cancelledBy) && customer.getFcmToken() != null && !customer.getFcmToken().isEmpty()) {
            try {
                this.fcmService.sendPushNotification(customer.getFcmToken(), customerNotiTitle, customerNotiBody);
                log.info("✅ FCM Cancel Push sent to Customer successfully.");
            } catch (Exception e) {
                log.error("❌ FCM Customer Cancel Push failed: {}", e.getMessage());
            }
        }

        // ==========================================
        // 🌟 2. STAFF DB NOTIFICATION & FCM PUSH
        // ==========================================
        if (staffToNotify != null) {
            User staffUser = staffToNotify.getUser();
            String staffNotiTitle = "Booking Cancelled 🔴";
            String staffNotiBody = "The booking for " + serviceName + " on " + bookingDateStr + " has been cancelled by Admin.";

            java.util.Map<String, Object> staffMetadata = new java.util.HashMap<>();
            staffMetadata.put("bookingId", savedBooking.getId().toString());
            staffMetadata.put("serviceName", serviceName);
            staffMetadata.put("bookingDate", bookingDateStr);
            staffMetadata.put("bookingStatus", "CANCELLED");
            staffMetadata.put("cancelledBy", "ADMIN");

            Notification staffDbNotification = Notification.builder()
                    .title(staffNotiTitle)
                    .message(staffNotiBody)
                    .type(NotificationType.BOOKING)
                    .targetAudience(TargetAudience.STAFF)
                    .bookingStatus(BookingStatus.CANCELLED)
                    .user(staffUser)
                    .metadata(staffMetadata)
                    .isRead(false)
                    .createdAt(Instant.now())
                    .build();
            this.notificationRepository.save(staffDbNotification);
            log.info("Cancellation Noti saved to DB for Staff: {}", staffUser.getEmail());

            if (staffUser.getFcmToken() != null && !staffUser.getFcmToken().isEmpty()) {
                try {
                    this.fcmService.sendPushNotification(staffUser.getFcmToken(), staffNotiTitle, staffNotiBody);
                    log.info("✅ FCM Cancel Push sent to Staff successfully.");
                } catch (Exception e) {
                    log.error("❌ FCM Staff Cancel Push failed: {}", e.getMessage());
                }
            }
        }

        // ==========================================
        // 🌟 3. REAL-TIME TOPIC PUSH FOR DASHBOARD
        // ==========================================
        try {
            fcmService.sendPushNotificationToTopic("booking-updates", "Booking Cancelled By Admin",
                    customer.getFullName() + " ၏ ဘွတ်ကင်ကို Admin မှ ပယ်ဖျက်လိုက်ပါသည်။");
        } catch (Exception e) {
            log.error("⚠️ Dashboard Topic Push bypassed: {}", e.getMessage());
        }

        BookingResponse response = this.mapToResponse(savedBooking);
        log.info("--- cancelBookingByAdmin process successfully finished ---");
        return response;
    }

    @Override
    @Transactional
    public BookingResponse cancelBookingByCustomer(Long id, String cancelledBy, String userEmail) {
        log.info("--- Starting cancelBookingByCustomer process for ID: {} ---", id);

        Booking booking = this.bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));

        if (!booking.getCustomer().getEmail().equals(userEmail)) {
            throw new BadRequestException("You are not authorized to cancel this booking.");
        }

        Instant now = Instant.now();
        Instant bookingTime = booking.getBookingDate();
        long hoursUntilBooking = Duration.between(now, bookingTime).toHours();
        if (hoursUntilBooking < 1L) {
            throw new BadRequestException("You can only cancel this booking at least 1 hour before the appointment.");
        }

        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BadRequestException("Only PENDING or CONFIRMED bookings can be cancelled.");
        }

        StaffProfile staffToNotify = booking.getAssignedStaff() != null
                ? booking.getAssignedStaff()
                : booking.getRequestedStaff();

        if (booking.getStaffAssignment() != null) {
            this.staffAssignmentRepository.delete(booking.getStaffAssignment());
            booking.setStaffAssignment(null);
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setAssignedStaff(null);
        booking.setCancelledBy(cancelledBy);

        Booking savedBooking = this.bookingRepository.save(booking);
        log.info("Booking ID: {} successfully cancelled by Customer.", id);

        String serviceName = savedBooking.getBusinessService().getName();
        String bookingDateStr = savedBooking.getBookingDate().toString();

        // =========================================================================
        // 1. CUSTOMER DB NOTIFICATION (ADMIN INBOX MAPS HERE VIA CUSTOMER PANEL)
        // =========================================================================
        User customer = savedBooking.getCustomer();
        String customerNotiTitle = "Booking Cancelled 🔴";
        String customerNotiBody = "Your appointment for " + serviceName + " has been successfully cancelled.";

        java.util.Map<String, Object> customerMetadata = new java.util.HashMap<>();
        customerMetadata.put("bookingId", savedBooking.getId().toString());
        customerMetadata.put("serviceName", serviceName);
        customerMetadata.put("bookingDate", bookingDateStr);
        customerMetadata.put("bookingStatus", "CANCELLED");

        Notification customerDbNotification = Notification.builder()
                .title(customerNotiTitle)
                .message(customerNotiBody)
                .type(NotificationType.BOOKING)
                .targetAudience(TargetAudience.CUSTOMER)
                .customerAction(CustomerAction.CANCELLED) // Maps to Admin Inbox Drawer (Customer -> Cancel Tab)
                .bookingStatus(BookingStatus.CANCELLED)
                .user(customer)
                .metadata(customerMetadata)
                .isRead(false)
                .createdAt(Instant.now())
                .build();
        this.notificationRepository.save(customerDbNotification);

        // =========================================================================
        // 2. STAFF DB NOTIFICATION & DYNAMIC FCM PUSH
        // =========================================================================
        if (staffToNotify != null) {
            User staffUser = staffToNotify.getUser();
            String staffNotiTitle = "Booking Cancelled by Customer";
            String staffNotiBody = "The booking for " + serviceName + " on " + bookingDateStr + " has been cancelled by the customer.";

            java.util.Map<String, Object> staffMetadata = new java.util.HashMap<>();
            staffMetadata.put("bookingId", savedBooking.getId().toString());
            staffMetadata.put("serviceName", serviceName);
            staffMetadata.put("bookingDate", bookingDateStr);
            staffMetadata.put("bookingStatus", "CANCELLED");
            staffMetadata.put("cancelledBy", "CUSTOMER");
            staffMetadata.put("customerName", customer.getFullName());

            Notification staffDbNotification = Notification.builder()
                    .title(staffNotiTitle)
                    .message(staffNotiBody)
                    .type(NotificationType.BOOKING)
                    .targetAudience(TargetAudience.STAFF)
                    .bookingStatus(BookingStatus.CANCELLED)
                    .user(staffUser)
                    .metadata(staffMetadata)
                    .isRead(false)
                    .createdAt(Instant.now())
                    .build();
            this.notificationRepository.save(staffDbNotification);
            log.info("Customer Cancellation Noti saved to DB for Staff: {}", staffUser.getEmail());

            if (staffUser.getFcmToken() != null && !staffUser.getFcmToken().isEmpty()) {
                try {
                    this.fcmService.sendPushNotification(staffUser.getFcmToken(), staffNotiTitle, staffNotiBody);
                    log.info("FCM Cancel Push sent to Staff successfully.");
                } catch (Exception e) {
                    log.error("FCM Staff Cancel Push failed: {}", e.getMessage());
                }
            }
        }

        // =========================================================================
        // 3. DYNAMIC PUSH TO ADMIN APP & REAL-TIME DASHBOARD TOPIC PUSH (ENGLISH)
        // =========================================================================
        try {
            // (A) Admin Mobile/Web App Banner Alerts
            String adminTopic = "admin-notifications";
            String adminPushTitle = "Customer Cancelled Booking 🚨";
            String adminPushBody = customer.getFullName() + " has cancelled their appointment for " + serviceName + ".";
            this.fcmService.sendPushNotificationToTopic(adminTopic, adminPushTitle, adminPushBody);
            log.info("FCM Push sent to Admin Topic successfully.");

            // (B) Admin Web Dashboard Real-time UI Updates
            String dashboardTopic = "booking-updates";
            String dashboardBody = customer.getFullName() + " has cancelled their booking for " + serviceName + ".";
            this.fcmService.sendPushNotificationToTopic(dashboardTopic, "Booking Cancelled By Customer", dashboardBody);
            log.info("Real-time UI Update Push sent to Dashboard Topic successfully.");

        } catch (Exception e) {
            log.error("Admin/Dashboard Topic Push failed or bypassed: {}", e.getMessage());
        }

        return this.mapToResponse(savedBooking);
    }

    @Transactional
    public void assignStaffToBooking(Long bookingId, Long staffId) {
        // 1. Booking အချက်အလက်ကို အရင်ဆွဲထုတ်မယ်
        Booking booking = this.bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

        // 2. Booking အချိန်ပေါ်မူတည်ပြီး Staff ရဲ့ အလုပ်ချိန် အပိုင်းအခြားကို တွက်ချက်မယ်
        Instant customerBookingTime = booking.getBookingDate();
        Instant staffStartTime = customerBookingTime.minus(10L, ChronoUnit.MINUTES);
        long duration = (long) booking.getBusinessService().getDurationInMinutes();
        Instant staffEndTime = customerBookingTime.plus(duration + 10L, ChronoUnit.MINUTES);

        StaffProfile staffProfile;

        // 🌟 3. Staff Assignment Flow ခွဲခြားခြင်း
        if (staffId != null) {
            // CASE A: Admin က လက်ရှိ Staff တစ်ယောက်ချင်းစီကို တိုက်ရိုက် Assign လုပ်တာ သို့မဟုတ် Customer က ရွေးလာတာ
            staffProfile = this.staffProfileRepository.findByUserId(staffId)
                    .orElseThrow(() -> new ResourceNotFoundException("Staff profile data is missing in DB for user id: " + staffId));

            boolean isBusy = this.staffAssignmentRepository.isStaffBusy(staffProfile.getUser().getId(), staffStartTime, staffEndTime);
            if (isBusy) {
                throw new BadRequestException("This staff member is busy or preparing for another customer at this time!");
            }
        } else {
            // CASE B: ဆရာကြီးအသစ်လိုချင်တဲ့ Flow (staffId က null ဖြစ်နေရင် အလိုအလျောက် ရှာပေးမှာပါ)
            // ယခင်အဆင့်မှာ ကျွန်တော်တို့ ရေးခဲ့တဲ့ Repository မက်သတ်ကို လှမ်းခေါ်ပါတယ်
            List<StaffProfile> availableStaff = this.staffProfileRepository.findAvailableStaffByTimeSlot(staffStartTime, staffEndTime);

            if (availableStaff.isEmpty()) {
                throw new BadRequestException("Booking Denied! No available staff found for this time slot.");
            }

            // 💡 တကယ်လို့ အားတဲ့သူတွေထဲကမှ လုံးဝ Random ဖြန့်ပေးချင်ရင် အောက်က line ကို uncomment ဖွင့်နိုင်ပါတယ်ဗျာ
            // java.util.Collections.shuffle(availableStaff);

            // အားနေတဲ့ Staff စာရင်းထဲက ပထမဆုံးတစ်ယောက်ကို ရွေးချယ်တာဝန်ပေးလိုက်ခြင်း
            staffProfile = availableStaff.get(0);
        }

        // 4. Booking မှာ တာဝန်ပေးထားပြီးသား အဟောင်းရှိရင် DB ထဲက အရင်ဖျက်ထုတ်ပေးခြင်း
        if (booking.getStaffAssignment() != null) {
            this.staffAssignmentRepository.delete(booking.getStaffAssignment());
        }

        // 5. Staff Assignment အသစ်ကို တည်ဆောက်ပြီး ချိတ်ဆက်ပေးခြင်း
        StaffAssignment assignment = StaffAssignment.builder()
                .staffProfile(staffProfile)
                .startTime(staffStartTime)
                .endTime(staffEndTime)
                .isBooked(true)
                .booking(booking)
                .build();

        booking.setAssignedStaff(staffProfile);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setStaffAssignment(assignment);

        // Database ထဲသို့ Update အချက်အလက်များ သိမ်းဆည်းခြင်း
        this.bookingRepository.save(booking);

        // 6. တာဝန်ကျသွားတဲ့ Staff ဆီသို့ FCM Push Notification ပေးပို့ခြင်း
        User staffUser = staffProfile.getUser();
        if (staffUser.getFcmToken() != null && !staffUser.getFcmToken().isEmpty()) {
            String title = "တာဝန်အသစ် ရောက်ရှိလာပါပြီ! 🛠️";
            String body = booking.getCustomer().getFullName() + " ၏ ဘွတ်ကင်အား သင့်ထံသို့ စနစ်မှ တာဝန်ပေးထားပါသည် ဆရာ။";

            try {
                this.fcmService.sendPushNotification(staffUser.getFcmToken(), title, body);
                log.info("✅ Staff FCM Sent Successfully to Staff: {}", staffUser.getEmail());
            } catch (Exception e) {
                log.error("❌ Failed to send FCM to staff: {}", e.getMessage());
            }
        }
    }

    @Override
    public List<CustomerStaffResponse> getStaffListForBooking(Long serviceId, Instant bookingDate) {
        // ၁။ ယူမည့် Service ၏ ကြာချိန်ကို ရှာဖွေခြင်း
        int duration = 60;
        if (serviceId != null) {
            BusinessService service = serviceRepository.findById(serviceId).orElse(null);
            if (service != null) {
                duration = service.getDurationInMinutes();
            }
        }

        // ၂။ Instant နှင့် Duration ကိုသုံး၍ ဝန်ထမ်းပြင်ဆင်ချိန် Bounds များကို တိုက်ရိုက်တွက်ချက်ခြင်း
        int bufferMinutes = 10;
        Instant customerStartTime = bookingDate;
        Instant customerEndTime = customerStartTime.plus(Duration.ofMinutes(duration));

        Instant staffStartTime = customerStartTime.minus(Duration.ofMinutes(bufferMinutes));
        Instant staffEndTime = customerEndTime.plus(Duration.ofMinutes(bufferMinutes));

        // Active ဖြစ်သော Staff များကို ယူခြင်း
        List<StaffProfile> activeProfiles = staffProfileRepository.findByIsAvailableTrue();

        // ၃။ Helper Method သို့ Instant Parameters များ ပေးပို့ခြင်း
        List<Booking> overlappingPendingBookings = getOverlappingPendingBookings(bookingDate, staffStartTime, staffEndTime, bufferMinutes);

        return activeProfiles.stream()
                // 🎯 🌟 Fix: ရွေးချယ်ထားသော ရက်တွင် Day Off/ခွင့် ရှိသည့် ဝန်ထမ်းများကို စာရင်းထဲမှ လုံးဝ ဖယ်ထုတ်ခြင်း
                .filter(profile -> !isStaffOnLeave(profile, bookingDate))
                .map(profile -> {
                    Long staffUserId = profile.getUser().getId();
                    Long staffProfileId = profile.getId();

                    boolean isStaffBusy = staffAssignmentRepository.isStaffBusy(staffUserId, staffStartTime, staffEndTime);
                    boolean isRequestedByOtherPending = overlappingPendingBookings.stream()
                            .anyMatch(pb -> pb.getRequestedStaff() != null && pb.getRequestedStaff().getUser().getId().equals(staffUserId));

                    boolean isAvailable = !isStaffBusy && !isRequestedByOtherPending;

                    long confirmedCount = staffAssignmentRepository.countConfirmedByStaffUserId(staffUserId);
                    long pendingCount = bookingRepository.countPendingByStaffProfileId(staffProfileId);
                    int totalBookingCount = (int) (confirmedCount + pendingCount);

                    return CustomerStaffResponse.builder()
                            .userId(staffUserId)
                            .staffProfileId(staffProfileId)
                            .fullName(profile.getUser().getFullName())
                            .profilePicture(profile.getUser().getProfilePicture())
                            .specializedName(profile.getSpecializedName() != null ? profile.getSpecializedName() : "Nail Artist")
                            .rating(profile.getRating())
                            .isAvailable(isAvailable)
                            .bookingCount(totalBookingCount)
                            .build();
                })
                .toList();
    }
    @Override
    @Transactional(readOnly = true)
    public List<HomeStaffResponse> getStaffListForHomePage() {
        // ၁။ ဝန်ထမ်းအားလုံးကို Database မှ ဆွဲယူခြင်း
        List<StaffProfile> allProfiles = staffProfileRepository.findAll();

        return allProfiles.stream()
                .map(profile -> {
                    // 🎯 🌟 Fix: Ranking စီရန်အတွက် အောင်မြင်စွာပြီးဆုံးခဲ့သော (COMPLETED) Booking များကိုသာ သီးသန့် ရေတွက်ခြင်း
                    int completedBookingCount = 0;
                    if (profile.getAssignedBookings() != null) {
                        completedBookingCount = (int) profile.getAssignedBookings().stream()
                                .filter(booking -> booking.getStatus() != null && "COMPLETED".equals(booking.getStatus().name()))
                                .count();
                    }

                    return HomeStaffResponse.builder()
                            .userId(profile.getUser().getId())
                            .staffProfileId(profile.getId())
                            .fullName(profile.getUser().getFullName())
                            .profilePicture(profile.getUser().getProfilePicture())
                            .specializedName(profile.getSpecializedName() != null ? profile.getSpecializedName() : "Nail Artist")
                            .rating(profile.getRating() != null ? profile.getRating() : 0.0)
                            .bookingCount(completedBookingCount) // 🌟 Completed Booking Count ကို ထည့်သွင်းခြင်း
                            .isAvailable(profile.isAvailable())
                            .build();
                })
                // 🎯 🌟 အဓိက ပြင်ဆင်ချက်: Multi-level Sorting (Ranking) ပြုလုပ်ခြင်း
                .sorted((staff1, staff2) -> {
                    // (က) ပထမဦးစားပေး - Rating မြင့်မားသူကို အပေါ်တွင် အရင်ပြရန် (Descending Order)
                    int ratingCompare = Double.compare(staff2.getRating(), staff1.getRating());
                    if (ratingCompare != 0) {
                        return ratingCompare;
                    }

                    // (ခ) ဒုတိယဦးစားပေး - Rating တူညီနေပါက Completed BookingCount များသူကို အပေါ်သို့ ပို့ရန် (Descending Order)
                    return Integer.compare(staff2.getBookingCount(), staff1.getBookingCount());
                })
                .toList();
    }

    @Override
    @Transactional
    public BookingResponse acceptBooking(Long id) {
        log.info("--- Starting acceptBooking process for ID: {} ---", id);
        Booking booking = this.bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (booking.getStatus() != BookingStatus.CONFIRMED && booking.getStatus() != BookingStatus.PENDING) {
            throw new BadRequestException("Only PENDING or CONFIRMED bookings can be accepted.");
        }

        User currentStaff = this.getCurrentAuthenticatedUser();
        StaffProfile currentStaffProfile = currentStaff.getStaffProfile();
        if (currentStaffProfile == null) {
            throw new ResourceNotFoundException("Staff profile not found for the authenticated account.");
        }

        boolean isAssignedStaff = booking.getAssignedStaff() != null && booking.getAssignedStaff().getId().equals(currentStaffProfile.getId());
        boolean isRequestedStaff = booking.getRequestedStaff() != null && booking.getRequestedStaff().getId().equals(currentStaffProfile.getId());

        if (isAssignedStaff || isRequestedStaff) {
            booking.setStatus(BookingStatus.IN_PROGRESS);

            if (booking.getAssignedStaff() == null) {
                booking.setAssignedStaff(currentStaffProfile);
            }

            if (booking.getStaffAssignment() != null) {
                booking.getStaffAssignment().setBooked(true);
            }

            Booking savedBooking = this.bookingRepository.save(booking);
            log.info("Booking ID: {} successfully accepted by Staff: {}", id, currentStaff.getFullName());

            String serviceName = savedBooking.getBusinessService().getName();
            String staffName = currentStaff.getFullName();
            String staffNotiTitle = "Booking Started by Staff 🟡";
            String staffNotiBody = "Staff " + staffName + " has started the service for " + serviceName + ".";

            // =========================================================================
            // 1. DB NOTIFICATION FOR ADMIN INBOX (STAFF PANEL -> IN_PROGRESS FILTER)
            // =========================================================================
            java.util.Map<String, Object> staffMetadata = new java.util.HashMap<>();
            staffMetadata.put("bookingId", savedBooking.getId().toString());
            staffMetadata.put("serviceName", serviceName);
            staffMetadata.put("bookingStatus", "IN_PROGRESS");
            staffMetadata.put("staffName", staffName);

            Notification adminStaffNotification = Notification.builder()
                    .title(staffNotiTitle)
                    .message(staffNotiBody)
                    .type(NotificationType.BOOKING)
                    .targetAudience(TargetAudience.STAFF)
                    .bookingStatus(BookingStatus.IN_PROGRESS)
                    .user(currentStaff)
                    .metadata(staffMetadata)
                    .isRead(false)
                    .createdAt(Instant.now())
                    .build();
            this.notificationRepository.save(adminStaffNotification);
            log.info("Staff Started Event Noti saved to DB for Admin Inbox.");

            // =========================================================================
            // 2. DYNAMIC FCM PUSH TO CUSTOMER
            // =========================================================================
            User customer = savedBooking.getCustomer();
            if (customer != null && customer.getFcmToken() != null && !customer.getFcmToken().isEmpty()) {
                try {
                    String customerPushTitle = "Service Started! ⚡";
                    String customerPushBody = "Your service for " + serviceName + " has been started by " + staffName + ".";
                    this.fcmService.sendPushNotification(customer.getFcmToken(), customerPushTitle, customerPushBody);
                    log.info("FCM Push sent to Customer successfully.");
                } catch (Exception e) {
                    log.error("FCM Customer Push failed: {}", e.getMessage());
                }
            }

            // =========================================================================
            // 3. DYNAMIC PUSH TO ADMIN APP & REAL-TIME DASHBOARD TOPIC PUSH
            // =========================================================================
            try {
                // Admin Mobile/Web App Banner Alerts
                String adminTopic = "admin-notifications";
                this.fcmService.sendPushNotificationToTopic(adminTopic, "Staff Started Booking ⏳", staffNotiBody);
                log.info("FCM Push sent to Admin Topic successfully.");

                // Admin Web Dashboard Real-time UI Updates
                String dashboardTopic = "booking-updates";
                this.fcmService.sendPushNotificationToTopic(dashboardTopic, "Booking In Progress", staffNotiBody);
                log.info("Real-time UI Update Push sent to Dashboard Topic successfully.");
            } catch (Exception e) {
                log.error("Admin/Dashboard Topic Push failed or bypassed: {}", e.getMessage());
            }

            return this.mapToResponse(savedBooking);
        } else {
            throw new BadRequestException("Unauthorized! You are not the assigned or requested staff member for this booking.");
        }
    }

    @Override
    @Transactional
    public BookingResponse completeBooking(Long id) {
        log.info("--- Starting completeBooking process for ID: {} ---", id);
        Booking booking = this.bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));

        if (booking.getStatus() != BookingStatus.IN_PROGRESS) {
            throw new BadRequestException("Service cannot be completed because it is not currently IN_PROGRESS.");
        }

        booking.setStatus(BookingStatus.COMPLETED);
        if (booking.getStaffAssignment() != null) {
            booking.getStaffAssignment().setBooked(false);
        }

        Booking updatedBooking = this.bookingRepository.save(booking);

        Payment payment = updatedBooking.getPayment();
        if (payment == null) {
            BigDecimal baseAmount = updatedBooking.getBusinessService().getPrice();
            payment = Payment.builder()
                    .booking(updatedBooking)
                    .baseAmount(baseAmount)
                    .extraAmount(BigDecimal.ZERO)
                    .amount(baseAmount)
                    .createdAt(Instant.now())
                    .build();
        }

        String generatedInvoiceNumber = "INV-" + updatedBooking.getId() + "-" + (System.currentTimeMillis() / 100000L);
        payment.setInvoiceNumber(generatedInvoiceNumber);
        payment.setStatus("PENDING");
        payment.setPaymentDate(Instant.now());
        this.paymentRepository.save(payment);

        String serviceName = updatedBooking.getBusinessService().getName();
        String staffName = updatedBooking.getAssignedStaff() != null ? updatedBooking.getAssignedStaff().getUser().getFullName() : "Staff";

        // =========================================================================
        // 1. DB NOTIFICATION FOR ADMIN INBOX (STAFF PANEL -> COMPLETED FILTER)
        // =========================================================================
        String adminNotiTitle = "Booking Completed 🎉";
        String adminNotiBody = "The service for " + serviceName + " has been successfully completed by " + staffName + ".";

        java.util.Map<String, Object> adminMetadata = new java.util.HashMap<>();
        adminMetadata.put("bookingId", updatedBooking.getId().toString());
        adminMetadata.put("serviceName", serviceName);
        adminMetadata.put("bookingStatus", "COMPLETED");
        adminMetadata.put("staffName", staffName);
        adminMetadata.put("invoiceNumber", generatedInvoiceNumber);

        Notification adminCompleteNotification = Notification.builder()
                .title(adminNotiTitle)
                .message(adminNotiBody)
                .type(NotificationType.BOOKING)
                .targetAudience(TargetAudience.STAFF)
                .bookingStatus(BookingStatus.COMPLETED)
                .user(updatedBooking.getAssignedStaff() != null ? updatedBooking.getAssignedStaff().getUser() : null)
                .metadata(adminMetadata)
                .isRead(false)
                .createdAt(Instant.now())
                .build();
        this.notificationRepository.save(adminCompleteNotification);
        log.info("Staff Completed Event Noti saved to DB for Admin Inbox.");

        // =========================================================================
        // 2. DYNAMIC FCM PUSH TO CUSTOMER
        // =========================================================================
        User customer = booking.getCustomer();
        if (customer != null && customer.getFcmToken() != null && !customer.getFcmToken().isEmpty()) {
            try {
                this.fcmService.sendPushNotification(
                        customer.getFcmToken(),
                        "Service Completed! 🎉",
                        "Your service has been successfully completed. Please proceed to the counter for checkout."
                );
                log.info("FCM Push sent to Customer for service completion.");
            } catch (Exception e) {
                log.error("FCM Customer Completion Push failed: {}", e.getMessage());
            }
        }

        // =========================================================================
        // 3. DYNAMIC PUSH TO ADMIN APP & REAL-TIME DASHBOARD TOPIC PUSH
        // =========================================================================
        try {
            // Admin Mobile/Web App Banner Alerts
            String adminTopic = "admin-notifications";
            this.fcmService.sendPushNotificationToTopic(adminTopic, "Booking Completed Alert ✅", adminNotiBody);
            log.info("FCM Push sent to Admin Topic successfully.");

            // Admin Web Dashboard Real-time UI Updates
            String dashboardTopic = "booking-updates";
            this.fcmService.sendPushNotificationToTopic(dashboardTopic, "Booking Completed", adminNotiBody);
            log.info("Real-time UI Update Push sent to Dashboard Topic successfully.");
        } catch (Exception e) {
            log.error("Admin/Dashboard Topic Push failed or bypassed: {}", e.getMessage());
        }

        return this.mapToResponse(updatedBooking);
    }

    @Transactional(readOnly = true)
    public InvoiceResponse generateInvoice(Long id) {
        Booking booking = this.bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));

        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new BadRequestException("Invoice can only be generated for COMPLETED bookings.");
        }

        Payment payment = booking.getPayment();
        if (payment == null) {
            throw new ResourceNotFoundException("Payment record not found for this booking.");
        }

        BigDecimal baseAmount = payment.getBaseAmount() != null ? payment.getBaseAmount() : BigDecimal.ZERO;
        BigDecimal extraAmount = payment.getExtraAmount() != null ? payment.getExtraAmount() : BigDecimal.ZERO;

        // 🌟 [UPDATED] Tax တွက်ချက်မှုအား ဖယ်ရှားပြီး Base Amount နှင့် Extra Amount ကိုသာ တိုက်ရိုက်ပေါင်းပါသည်
        BigDecimal totalAmount = baseAmount.add(extraAmount);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm a").withZone(ZoneId.of("Asia/Yangon"));

        return InvoiceResponse.builder()
                .invoiceNumber(payment.getInvoiceNumber())
                .bookingId(booking.getId())
                .customerName(booking.getCustomer().getFullName())
                .customerPhone(booking.getCustomer().getPhone())
                .staffName(booking.getAssignedStaff() != null ? booking.getAssignedStaff().getUser().getFullName() : "N/A")
                .serviceName(booking.getBusinessService().getName())
                .price(baseAmount)
                .totalAmount(totalAmount)
                .completedAt(formatter.format(payment.getPaymentDate() != null ? payment.getPaymentDate() : Instant.now()))
                .build();
    }

    public List<StaffDutyResponse> getStaffDailyDuties(LocalDate selectedDate, BookingStatus status) {
        User currentStaffUser = this.getCurrentAuthenticatedUser();
        StaffProfile staffProfile = this.staffProfileRepository.findByUserId(currentStaffUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Staff Profile not found"));

        ZoneId yangonZone = ZoneId.of("Asia/Yangon");
        Instant startOfDay = selectedDate.atStartOfDay(yangonZone).toInstant();
        Instant endOfDay = selectedDate.atTime(LocalTime.MAX).atZone(yangonZone).toInstant();

        List<Booking> bookings = this.bookingRepository.findStaffDutiesByDateAndStatus(staffProfile.getId(), startOfDay, endOfDay, status);

        return bookings.stream()
                .map(b -> {
                    StaffDutyResponse dto = new StaffDutyResponse();
                    dto.setBookingId(b.getId());
                    dto.setCustomerName(b.getCustomer().getFullName());
                    dto.setServiceName(b.getBusinessService().getName());
                    dto.setBookingDate(b.getBookingDate());
                    dto.setDurationInMinutes(b.getBusinessService().getDurationInMinutes());
                    dto.setStatus(b.getStatus());
                    dto.setNotes(b.getNotes());
                    return dto;
                })
                .toList(); // 🧹 Modern Java style သို့ ပြောင်းလဲခြင်း
    }

    public StaffHistoryResponse getStaffWorkHistory(HistoryFilter filter) {
        User currentStaffUser = this.getCurrentAuthenticatedUser();
        StaffProfile staffProfile = this.staffProfileRepository.findByUserId(currentStaffUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Staff Profile not found"));

        ZoneId yangonZone = ZoneId.of("Asia/Yangon");
        ZonedDateTime now = ZonedDateTime.now(yangonZone);
        Instant startInstant;
        Instant endInstant;

        switch (filter) {
            case TODAY -> {
                startInstant = now.with(LocalTime.MIN).toInstant();
                endInstant = now.with(LocalTime.MAX).toInstant();
            }
            case WEEKLY -> {
                ZonedDateTime startOfWeek = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).with(LocalTime.MIN);
                ZonedDateTime endOfWeek = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).with(LocalTime.MAX);
                startInstant = startOfWeek.toInstant();
                endInstant = endOfWeek.toInstant();
            }
            case MONTHLY -> {
                ZonedDateTime startOfMonth = now.with(TemporalAdjusters.firstDayOfMonth()).with(LocalTime.MIN);
                ZonedDateTime endOfMonth = now.with(TemporalAdjusters.lastDayOfMonth()).with(LocalTime.MAX);
                startInstant = startOfMonth.toInstant();
                endInstant = endOfMonth.toInstant();
            }
            default -> throw new BadRequestException("Invalid Filter Type");
        }

        List<Booking> completedBookings = this.bookingRepository.findStaffHistory(staffProfile.getId(), BookingStatus.COMPLETED, startInstant, endInstant);

        // 🧹 Stream mapping အား ပိုမိုရှင်းလင်းအောင် ပြုပြင်ခြင်း
        List<StaffHistoryDetailResponse> details = completedBookings.stream()
                .map(b -> {
                    StaffHistoryDetailResponse item = new StaffHistoryDetailResponse();
                    item.setBookingId(b.getId());
                    item.setServiceName(b.getBusinessService().getName());
                    item.setCustomerName(b.getCustomer().getFullName());
                    item.setBookingDate(b.getBookingDate());

                    // ဝန်ထမ်း ကော်မရှင် ၁၀ ရာခိုင်နှုန်း တွက်ချက်ခြင်း
                    BigDecimal commissionPercent = new BigDecimal("0.10");
                    BigDecimal servicePrice = b.getBusinessService().getPrice();
                    item.setCommission(servicePrice != null ? servicePrice.multiply(commissionPercent) : BigDecimal.ZERO);
                    return item;
                })
                .toList();

        BigDecimal totalCommissionSum = details.stream()
                .map(StaffHistoryDetailResponse::getCommission)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        StaffHistoryResponse response = new StaffHistoryResponse();
        response.setTotalJobsDone((long) details.size());
        response.setTotalCommission(totalCommissionSum);
        response.setHistoryList(details);
        return response;
    }

    private User getCurrentAuthenticatedUser() {
        String testEmail = this.httpServletRequest.getHeader("X-Test-Email");
        if (testEmail != null && !testEmail.isEmpty()) {
            return this.userRepository.findByEmail(testEmail)
                    .orElseThrow(() -> new ResourceNotFoundException("Test User not found with email: " + testEmail));
        }

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return this.userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Authenticated user not found"));
    }

    private boolean isStaffOnLeave(StaffProfile profile, Instant bookingDate) {
        if (profile.getLeaves() == null || profile.getLeaves().isEmpty()) {
            return false;
        }
        // 🎯 ဝန်ထမ်း၏ ခွင့်ရက်များထဲတွင် ရွေးချယ်ထားသော bookingDate ငြိနေခြင်း ရှိ/မရှိ စစ်ဆေးခြင်း
        return profile.getLeaves().stream().anyMatch(leave ->
                !bookingDate.isBefore(leave.getStartDate()) &&
                        (leave.getEndDate() == null || !bookingDate.isAfter(leave.getEndDate()))
        );
    }

    private BookingResponse mapToResponse(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .customerId(booking.getCustomer().getId())
                .customerName(booking.getCustomer().getFullName())
                .customerPhone(booking.getCustomer().getPhone())
                .serviceId(booking.getBusinessService().getId())
                .serviceName(booking.getBusinessService().getName())
                .bookingDate(booking.getBookingDate())
                .notes(booking.getNotes())
                .status(booking.getStatus())
                .createdByCustomerOrStaffName(booking.getCreatedBy().getFullName())
                .createdAt(booking.getCreatedAt())
                .cancelledBy(booking.getCancelledBy())
                .rejectionReason(booking.getRejectionReason())
                .build();
    }
}
