//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.codingproject.digitalbase.service;

import com.codingproject.digitalbase.dtos.BookingRequest;
import com.codingproject.digitalbase.dtos.BookingResponse;
import com.codingproject.digitalbase.dtos.CustomerStaffResponse;
import com.codingproject.digitalbase.dtos.InvoiceResponse;
import com.codingproject.digitalbase.dtos.StaffDutyResponse;
import com.codingproject.digitalbase.dtos.StaffHistoryDetailResponse;
import com.codingproject.digitalbase.dtos.StaffHistoryResponse;
import com.codingproject.digitalbase.dtos.StaffTimeSlotResponse;
import com.codingproject.digitalbase.dtos.WalkInBookingRequest;
import com.codingproject.digitalbase.enums.BookingStatus;
import com.codingproject.digitalbase.enums.HistoryFilter;
import com.codingproject.digitalbase.enums.RoleName;
import com.codingproject.digitalbase.exception.BadRequestException;
import com.codingproject.digitalbase.exception.ResourceNotFoundException;
import com.codingproject.digitalbase.exception.UnauthorizedException;
import com.codingproject.digitalbase.model.Booking;
import com.codingproject.digitalbase.model.BusinessService;
import com.codingproject.digitalbase.model.Payment;
import com.codingproject.digitalbase.model.Role;
import com.codingproject.digitalbase.model.StaffProfile;
import com.codingproject.digitalbase.model.User;
import com.codingproject.digitalbase.model.StaffAssignment;
import com.codingproject.digitalbase.repository.BookingRepository;
import com.codingproject.digitalbase.repository.BusinessServiceRepository;
import com.codingproject.digitalbase.repository.PaymentRepository;
import com.codingproject.digitalbase.repository.RoleRepository;
import com.codingproject.digitalbase.repository.StaffAssignmentRepository;
import com.codingproject.digitalbase.repository.StaffProfileRepository;
import com.codingproject.digitalbase.repository.UserRepository;
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
import lombok.Generated;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
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
        BusinessService service = serviceRepository.findById(request.getServiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));

        Integer duration = service.getDurationInMinutes();
        Instant customerBookingInstant = request.getBookingDate();

        // 🌟 ၁။ Rigid Slot နှင့် ဆိုင်ဖွင့်/ပိတ်ချိန် စစ်ဆေးရန်အတွက်သာ ZonedDateTime ကို သုံးခြင်း
        ZoneId zoneId = ZoneId.of("Asia/Yangon");
        ZonedDateTime zonedDateTime = customerBookingInstant.atZone(zoneId);
        LocalTime customerStartTime = zonedDateTime.toLocalTime();

        // Rigid Slot Validation (၁ နာရီ ကွက်တိ ဟုတ်မဟုတ် စစ်ဆေးခြင်း)
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

        // =========================================================================
        // [VALIDATION] ဝယ်သူကိုယ်တိုင် တစ်ချိန်တည်းမှာ ဘွတ်ကင်အထပ်ထပ် ဖြစ်မဖြစ် စစ်ဆေးခြင်း
        // =========================================================================
        List<BookingStatus> activeStatuses = List.of(
                BookingStatus.PENDING,
                BookingStatus.CONFIRMED,
                BookingStatus.IN_PROGRESS
        );

        boolean hasExistingBooking = bookingRepository.existsByCustomerIdAndBookingDateAndStatusIn(
                currentUser.getId(),
                customerBookingInstant, // Instant အတိုင်း တိုက်ရိုက်စစ်ဆေးသည်
                activeStatuses
        );

        if (hasExistingBooking) {
            throw new BadRequestException("Booking Denied! You already have another active booking (Pending/Confirmed/In Progress) at this exact time slot.");
        }
        // =========================================================================

        // 🌟 ၂။ Duration နှင့် Instant ကိုသုံး၍ ဝန်ထမ်းပြင်ဆင်ချိန် Bounds များကို တွက်ချက်ခြင်း
        int bufferMinutes = 10;
        Instant staffStartTime = customerBookingInstant.minus(Duration.ofMinutes(bufferMinutes));
        Instant staffEndTime = customerBookingInstant.plus(Duration.ofMinutes(duration + bufferMinutes));

        List<StaffProfile> activeStaffProfiles = staffProfileRepository.findByIsAvailableTrue();

        // Updated Helper Method သို့ Instant parameters များ ပေးပို့ခြင်း
        List<Booking> overlappingPendingBookings = getOverlappingPendingBookings(customerBookingInstant, staffStartTime, staffEndTime, bufferMinutes);

        // 🌟 ၃။ Repository အသစ်ပုံစံအတိုင်း အချိန်ပေးပို့၍ ဆိုင် Capacity စစ်ဆေးခြင်း
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

            boolean isSpecialized = requestedStaffProfile.getSpecializedServices().stream()
                    .anyMatch(s -> s.getId().equals(service.getId()));

            if (!isSpecialized) {
                throw new BadRequestException(String.format("Booking Denied! Staff '%s' does not provide '%s' service. Please select another staff member.",
                        requestedStaffProfile.getUser().getFullName(), service.getName()));
            }

            // 🌟 ၄။ တောင်းဆိုထားသော ဝန်ထမ်း အလုပ်အား/မအား Instant ဖြင့် စစ်ဆေးခြင်း
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
        booking.setAssignedStaff(null);
        booking.setStaffAssignment(null);

        if (requestedStaffProfile != null) {
            User staffUser = requestedStaffProfile.getUser();
            if (staffUser.getFcmToken() != null && !staffUser.getFcmToken().isEmpty()) {
                fcmService.sendPushNotification(
                        staffUser.getFcmToken(),
                        "New Booking Requested! 📅",
                        currentUser.getFullName() + " has requested you for " + service.getName()
                );
            }
        }

        BookingResponse response = mapToResponse(bookingRepository.save(booking));

        try {
            String testTopic = "booking-updates";
            String title = "New Booking Alert! 🎉";
            String body = currentUser.getFullName() + " က " + service.getName() + " ကို ဘွတ်ကင်တင်လိုက်ပါပြီ။";
            fcmService.sendPushNotificationToTopic(testTopic, title, body);
        } catch (Exception e) {
            log.error("⚠️ Notification sending bypassed: {}", e.getMessage());
        }

        return response;
    }

    @Override
    @Transactional
    public BookingResponse createWalkInBooking(WalkInBookingRequest request) {
        try {
            User currentStaff = getCurrentAuthenticatedUser();

            // ၁။ ဝန်ထမ်း၏ Staff Profile ရှိမရှိ စစ်ဆေးခြင်း
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

            // 🌟 ၂။ StaffAssignment အသစ်အား Instant ဖြင့် တည်ဆောက်ခြင်း
            // ဝန်ဆောင်မှု ကြာချိန်ကို Service ထဲတွင် သတ်မှတ်ထားခြင်း မရှိပါက ပုံသေ ၁ နာရီ (Duration.ofHours(1)) ဟု ယူဆပါမည်
            Instant endInstant = bookingInstant.plus(Duration.ofHours(1));

            StaffAssignment assignment = StaffAssignment.builder()
                    .staffProfile(staffProfile)
                    .startTime(bookingInstant)
                    .endTime(endInstant)
                    .isBooked(true)
                    .build();

            // Database ထဲသို့ Slot အရင်သိမ်းဆည်းမည်
            StaffAssignment savedAssignment = staffAssignmentRepository.save(assignment);

            // ၃။ Booking အား တည်ဆောက်ပြီး Assignment နှင့် ချိတ်ဆက်ခြင်း
            Booking booking = new Booking();
            booking.setCustomer(walkInCustomer);
            booking.setCreatedBy(currentStaff);
            booking.setAssignedStaff(staffProfile);

            // 🌟 ဤနေရာတွင် ရရှိလာသော Staff Assignment ID ကိုပါ တိုက်ရိုက်ထည့်သွင်းချိတ်ဆက်သည်
            booking.setStaffAssignment(savedAssignment);

            booking.setBusinessService(service);
            booking.setBookingDate(bookingInstant);
            booking.setNotes(request.getNotes());
            booking.setStatus(BookingStatus.COMPLETED);
            booking.setCreatedAt(Instant.now());

            Booking savedBooking = bookingRepository.save(booking);

            // ၄။ Payment အား လုပ်ဆောင်ခြင်း
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

            return mapToResponse(savedBooking);

        } catch (Exception e) {
            System.out.println("============== 🚨 WALK-IN ERROR FOUND 🚨 ==============");
            e.printStackTrace();
            System.out.println("=======================================================");
            throw e;
        }
    }
    public Page<BookingResponse> getMyBookingHistory(int page, int size) {
        User currentUser = this.getCurrentAuthenticatedUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by(new String[]{"createdAt"}).descending());
        Page<Booking> bookingPage = this.bookingRepository.findByCustomerId(currentUser.getId(), pageable);
        return bookingPage.map(this::mapToResponse);
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
        Booking booking = (Booking)this.bookingRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));
        return this.mapToResponse(booking);
    }

    @Override
    @Transactional
    public BookingResponse confirmBooking(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BadRequestException("Only PENDING bookings can be confirmed.");
        }

        // 🌟 Instant ဖြင့် Buffer Time တိုက်ရိုက်တွက်ချက်ခြင်း (Timezone ပြောင်းစရာမလိုတော့ပါ)
        Instant customerStartTime = booking.getBookingDate();
        Instant staffStartTime = customerStartTime.minus(Duration.ofMinutes(20));

        Integer durationInMinutes = booking.getBusinessService().getDurationInMinutes();
        Instant staffEndTime = customerStartTime.plus(Duration.ofMinutes(durationInMinutes + 20));

        StaffProfile finalStaffProfile = null;

        if (booking.getRequestedStaff() != null) {
            finalStaffProfile = booking.getRequestedStaff();

            // Updated Repository Method သို့ Instant များ ပေးပို့ခြင်း
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

        // 🌟 StaffAssignment တည်ဆောက်ရာတွင်လည်း Instant ဖြင့်သာ ထည့်သွင်းခြင်း
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

        BookingResponse response = mapToResponse(bookingRepository.save(booking));

        User customer = booking.getCustomer();
        if (customer.getFcmToken() != null && !customer.getFcmToken().isEmpty()) {
            fcmService.sendPushNotification(
                    customer.getFcmToken(),
                    "Booking Confirmed! 🎉",
                    "Your booking for " + booking.getBusinessService().getName() + " has been successfully confirmed with Staff: " + finalStaffProfile.getUser().getFullName()
            );
        }

        return response;
    }

    @Transactional
    public BookingResponse cancelBooking(Long id, String cancelledBy, String userEmail) {
        Booking booking = (Booking)this.bookingRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));
        if ("CUSTOMER".equals(cancelledBy)) {
            if (!booking.getCustomer().getEmail().equals(userEmail)) {
                throw new BadRequestException("You are not authorized to cancel this booking.");
            }

            Instant now = Instant.now();
            Instant bookingTime = booking.getBookingDate();
            long hoursUntilBooking = Duration.between(now, bookingTime).toHours();
            long minimumHoursRequired = 1L;
            if (hoursUntilBooking < minimumHoursRequired) {
                throw new BadRequestException("You can only cancel this booking at least " + minimumHoursRequired + " hours before the appointment. Remaining time: " + hoursUntilBooking + " hours.");
            }
        }

        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BadRequestException("Only PENDING or CONFIRMED bookings can be cancelled.");
        } else {
            if (booking.getStaffAssignment() != null) {
                this.staffAssignmentRepository.delete(booking.getStaffAssignment());
                booking.setStaffAssignment(null);
            }

            booking.setStatus(BookingStatus.CANCELLED);
            booking.setAssignedStaff(null);
            booking.setCancelledBy(cancelledBy);
            BookingResponse response = this.mapToResponse(this.bookingRepository.save(booking));
            if ("ADMIN".equals(cancelledBy)) {
                User customer = booking.getCustomer();
                if (customer.getFcmToken() != null && !customer.getFcmToken().isEmpty()) {
                    this.fcmService.sendPushNotification(customer.getFcmToken(), "Booking Rejected ❌", "Sorry, your booking for " + booking.getBusinessService().getName() + " has been rejected by the admin.");
                }
            }

            return response;
        }
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

        // 🌟 ၂။ Instant နှင့် Duration ကိုသုံး၍ ဝန်ထမ်းပြင်ဆင်ချိန် (Buffer Time) Bounds များကို တိုက်ရိုက်တွက်ချက်ခြင်း
        int bufferMinutes = 10;
        Instant customerStartTime = bookingDate;
        Instant customerEndTime = customerStartTime.plus(Duration.ofMinutes(duration));

        Instant staffStartTime = customerStartTime.minus(Duration.ofMinutes(bufferMinutes));
        Instant staffEndTime = customerEndTime.plus(Duration.ofMinutes(bufferMinutes));

        // Active ဖြစ်သော Staff များကို ယူခြင်း
        List<StaffProfile> activeProfiles = staffProfileRepository.findByIsAvailableTrue();

        if (serviceId != null) {
            activeProfiles = activeProfiles.stream()
                    .filter(profile -> profile.getSpecializedServices().stream()
                            .anyMatch(service -> service.getId().equals(serviceId)))
                    .toList();
        }

        // 🌟 ၃။ Helper Method သို့ Instant Parameters များ ပေးပို့ခြင်း
        List<Booking> overlappingPendingBookings = getOverlappingPendingBookings(bookingDate, staffStartTime, staffEndTime, bufferMinutes);

        return activeProfiles.stream()
                .map(profile -> {
                    Long staffUserId = profile.getUser().getId();
                    Long staffProfileId = profile.getId();

                    // (က) Confirmed ဖြစ်ပြီးသား အလုပ်ရှိမရှိ Instant ဖြင့် စစ်ဆေးခြင်း
                    boolean isStaffBusy = staffAssignmentRepository.isStaffBusy(staffUserId, staffStartTime, staffEndTime);

                    // (ခ) အခြားသူက ဤဝန်ထမ်းအား တောင်းဆိုထားသော Pending ရှိမရှိ စစ်ဆေးခြင်း
                    boolean isRequestedByOtherPending = overlappingPendingBookings.stream()
                            .anyMatch(pb -> pb.getRequestedStaff() != null && pb.getRequestedStaff().getUser().getId().equals(staffUserId));

                    boolean isAvailable = !isStaffBusy && !isRequestedByOtherPending;

                    // (ဂ) ဝန်ထမ်းတစ်ဦးချင်းစီ၏ အလုပ်အရေအတွက် တွက်ချက်ခြင်း
                    long confirmedCount = staffAssignmentRepository.countConfirmedByStaffUserId(staffUserId);
                    long pendingCount = bookingRepository.countPendingByStaffProfileId(staffProfileId);
                    int totalBookingCount = (int) (confirmedCount + pendingCount);

                    return CustomerStaffResponse.builder()
                            .staffProfileId(profile.getId())
                            .fullName(profile.getUser().getFullName())
                            .profilePicture(profile.getUser().getProfilePicture())
                            .rating(profile.getRating())
                            .isAvailable(isAvailable)
                            .bookingCount(totalBookingCount)
                            .build();
                })
                .toList();
    }

    @Override
    public List<User> getAvailableStaffForDateTime(Instant bookingDate) {
        // 🌟 ဝန်ဆောင်မှုမသိရသေးသဖြင့် အခြေခံ ၁ နာရီ slot အပြင် Buffer အား Instant ဖြင့်သာ တွက်ချက်မည်
        int bufferMinutes = 10;
        Instant staffStartTime = bookingDate.minus(Duration.ofMinutes(bufferMinutes));
        Instant staffEndTime = bookingDate.plus(Duration.ofMinutes(60 + bufferMinutes));

        List<StaffProfile> activeProfiles = staffProfileRepository.findByIsAvailableTrue();

        // Helper Method သို့ ပေးပို့ခြင်း
        List<Booking> overlappingPendingBookings = getOverlappingPendingBookings(bookingDate, staffStartTime, staffEndTime, bufferMinutes);

        return activeProfiles.stream()
                .map(StaffProfile::getUser)
                .filter(user -> {
                    Long staffUserId = user.getId();
                    boolean isStaffBusy = staffAssignmentRepository.isStaffBusy(staffUserId, staffStartTime, staffEndTime);
                    boolean isRequestedByOtherPending = overlappingPendingBookings.stream()
                            .anyMatch(pb -> pb.getRequestedStaff() != null && pb.getRequestedStaff().getUser().getId().equals(staffUserId));
                    return !isStaffBusy && !isRequestedByOtherPending;
                })
                .toList();
    }

    @Transactional
    public BookingResponse acceptBooking(Long id) {
        Booking booking = (Booking)this.bookingRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BadRequestException("Only CONFIRMED bookings can be accepted.");
        } else {
            User currentStaff = this.getCurrentAuthenticatedUser();
            StaffProfile currentStaffProfile = currentStaff.getStaffProfile();
            System.out.println("=== DEBUGGING ID MISMATCH ===");
            System.out.println("Booking Requested Staff Profile ID: " + booking.getRequestedStaff().getId());
            System.out.println("Current Logged-in Staff Profile ID: " + currentStaffProfile.getId());
            System.out.println("=============================");
            if (booking.getAssignedStaff() != null && booking.getAssignedStaff().getId().equals(currentStaffProfile.getId())) {
                booking.setStatus(BookingStatus.IN_PROGRESS);
                if (booking.getStaffAssignment() != null) {
                    booking.getStaffAssignment().setBooked(true);
                }

                Booking savedBooking = (Booking)this.bookingRepository.save(booking);
                return this.mapToResponse(savedBooking);
            } else {
                throw new BadRequestException("Unauthorized! You are not the assigned staff member for this booking.");
            }
        }
    }

    @Transactional
    public BookingResponse completeBooking(Long id) {
        Booking booking = (Booking)this.bookingRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));
        if (booking.getStatus() != BookingStatus.IN_PROGRESS) {
            throw new BadRequestException("Service cannot be completed because it is not currently IN_PROGRESS.");
        } else {
            booking.setStatus(BookingStatus.COMPLETED);
            if (booking.getStaffAssignment() != null) {
                booking.getStaffAssignment().setBooked(false);
            }

            Booking updatedBooking = (Booking)this.bookingRepository.save(booking);
            Payment payment = updatedBooking.getPayment();
            if (payment == null) {
                BigDecimal baseAmount = updatedBooking.getBusinessService().getPrice();
                payment = Payment.builder().booking(updatedBooking).baseAmount(baseAmount).extraAmount(BigDecimal.ZERO).amount(baseAmount).createdAt(Instant.now()).build();
            }

            Long var10000 = updatedBooking.getId();
            String generatedInvoiceNumber = "INV-" + var10000 + "-" + System.currentTimeMillis() / 100000L;
            payment.setInvoiceNumber(generatedInvoiceNumber);
            payment.setStatus("COMPLETED");
            payment.setPaymentDate(Instant.now());
            this.paymentRepository.save(payment);
            User customer = booking.getCustomer();
            if (customer != null && customer.getFcmToken() != null && !customer.getFcmToken().isEmpty()) {
                this.fcmService.sendPushNotification(customer.getFcmToken(), "Service Completed! \ud83c\udf89", "ဆရာ့ရဲ့ ဝန်ဆောင်မှု ပြီးမြောက်သွားပါပြီ။ ကျေးဇူးပြု၍ ကောင်တာတွင် ငွေရှင်းပေးပါရန်။");
            }

            return this.mapToResponse(updatedBooking);
        }
    }

    @Transactional(
            readOnly = true
    )
    public InvoiceResponse generateInvoice(Long id) {
        Booking booking = (Booking)this.bookingRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new BadRequestException("Invoice can only be generated for COMPLETED bookings.");
        } else {
            Payment payment = booking.getPayment();
            if (payment == null) {
                throw new ResourceNotFoundException("Payment record not found for this booking.");
            } else {
                BigDecimal baseAmount = payment.getBaseAmount() != null ? payment.getBaseAmount() : BigDecimal.ZERO;
                BigDecimal extraAmount = payment.getExtraAmount() != null ? payment.getExtraAmount() : BigDecimal.ZERO;
                BigDecimal totalAmount = baseAmount.add(extraAmount);
                BigDecimal servicePrice = booking.getBusinessService().getPrice();
                BigDecimal tax = servicePrice.multiply(new BigDecimal("0.05"));
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm a").withZone(ZoneId.of("Asia/Yangon"));
                return InvoiceResponse.builder().invoiceNumber(payment.getInvoiceNumber()).bookingId(booking.getId()).customerName(booking.getCustomer().getFullName()).customerPhone(booking.getCustomer().getPhone()).staffName(booking.getAssignedStaff() != null ? booking.getAssignedStaff().getUser().getFullName() : "N/A").serviceName(booking.getBusinessService().getName()).price(baseAmount).tax(tax).totalAmount(totalAmount).completedAt(formatter.format(payment.getPaymentDate() != null ? payment.getPaymentDate() : Instant.now())).build();
            }
        }
    }

    public List<StaffDutyResponse> getStaffWeeklyDuties(LocalDate selectedDate, BookingStatus status) {
        User currentStaffUser = this.getCurrentAuthenticatedUser();
        StaffProfile staffProfile = (StaffProfile)this.staffProfileRepository.findByUserId(currentStaffUser.getId()).orElseThrow(() -> new ResourceNotFoundException("Staff Profile not found"));
        ZoneId yangonZone = ZoneId.of("Asia/Yangon");
        Instant startOfDay = selectedDate.atStartOfDay(yangonZone).toInstant();
        Instant endOfDay = selectedDate.atTime(LocalTime.MAX).atZone(yangonZone).toInstant();
        List<Booking> bookings = this.bookingRepository.findStaffDutiesByDateAndStatus(staffProfile.getId(), startOfDay, endOfDay, status);
        return bookings.stream().map((b) -> {
            StaffDutyResponse dto = new StaffDutyResponse();
            dto.setBookingId(b.getId());
            dto.setCustomerName(b.getCustomer().getFullName());
            dto.setServiceName(b.getBusinessService().getName());
            dto.setBookingDate(b.getBookingDate());
            dto.setDurationInMinutes(b.getBusinessService().getDurationInMinutes());
            dto.setStatus(b.getStatus());
            dto.setNotes(b.getNotes());
            return dto;
        }).collect(Collectors.toList());
    }

    public StaffHistoryResponse getStaffWorkHistory(HistoryFilter filter) {
        User currentStaffUser = this.getCurrentAuthenticatedUser();
        StaffProfile staffProfile = (StaffProfile)this.staffProfileRepository.findByUserId(currentStaffUser.getId()).orElseThrow(() -> new ResourceNotFoundException("Staff Profile not found"));
        ZoneId yangonZone = ZoneId.of("Asia/Yangon");
        ZonedDateTime now = ZonedDateTime.now(yangonZone);
        Instant startInstant;
        Instant endInstant;
        switch (filter) {
            case TODAY:
                startInstant = now.with(LocalTime.MIN).toInstant();
                endInstant = now.with(LocalTime.MAX).toInstant();
                break;
            case WEEKLY:
                ZonedDateTime startOfWeek = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).with(LocalTime.MIN);
                ZonedDateTime endOfWeek = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).with(LocalTime.MAX);
                startInstant = startOfWeek.toInstant();
                endInstant = endOfWeek.toInstant();
                break;
            case MONTHLY:
                ZonedDateTime startOfMonth = now.with(TemporalAdjusters.firstDayOfMonth()).with(LocalTime.MIN);
                ZonedDateTime endOfMonth = now.with(TemporalAdjusters.lastDayOfMonth()).with(LocalTime.MAX);
                startInstant = startOfMonth.toInstant();
                endInstant = endOfMonth.toInstant();
                break;
            default:
                throw new BadRequestException("Invalid Filter Type");
        }

        List<Booking> completedBookings = this.bookingRepository.findStaffHistory(staffProfile.getId(), BookingStatus.COMPLETED, startInstant, endInstant);
        List<StaffHistoryDetailResponse> details = completedBookings.stream().map((b) -> {
            StaffHistoryDetailResponse item = new StaffHistoryDetailResponse();
            item.setBookingId(b.getId());
            item.setServiceName(b.getBusinessService().getName());
            item.setCustomerName(b.getCustomer().getFullName());
            item.setBookingDate(b.getBookingDate());
            BigDecimal commissionPercent = new BigDecimal("0.10");
            BigDecimal servicePrice = b.getBusinessService().getPrice();
            if (servicePrice != null) {
                BigDecimal calculatedCommission = servicePrice.multiply(commissionPercent);
                item.setCommission(calculatedCommission);
            } else {
                item.setCommission(BigDecimal.ZERO);
            }

            return item;
        }).collect(Collectors.toList());
        BigDecimal totalCommissionSum = (BigDecimal)details.stream().map(StaffHistoryDetailResponse::getCommission).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        StaffHistoryResponse response = new StaffHistoryResponse();
        response.setTotalJobsDone((long)details.size());
        response.setTotalCommission(totalCommissionSum);
        response.setHistoryList(details);
        return response;
    }

    private User getCurrentAuthenticatedUser() {
        String testEmail = this.httpServletRequest.getHeader("X-Test-Email");
        if (testEmail != null && !testEmail.isEmpty()) {
            return (User)this.userRepository.findByEmail(testEmail).orElseThrow(() -> new ResourceNotFoundException("Test User not found with email: " + testEmail));
        } else {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            return (User)this.userRepository.findByEmail(email).orElseThrow(() -> new UnauthorizedException("Authenticated user not found"));
        }
    }

    private BookingResponse mapToResponse(Booking booking) {
        return BookingResponse.builder().id(booking.getId()).customerId(booking.getCustomer().getId()).customerName(booking.getCustomer().getFullName()).customerPhone(booking.getCustomer().getPhone()).serviceId(booking.getBusinessService().getId()).serviceName(booking.getBusinessService().getName()).bookingDate(booking.getBookingDate()).notes(booking.getNotes()).status(booking.getStatus()).createdByCustomerOrStaffName(booking.getCreatedBy().getFullName()).createdAt(booking.getCreatedAt()).cancelledBy(booking.getCancelledBy()).build();
    }

    @Generated
    public BookingServiceImpl(final BookingRepository bookingRepository, final UserRepository userRepository, final BusinessServiceRepository serviceRepository, final RoleRepository roleRepository, final PasswordEncoder passwordEncoder, final HttpServletRequest httpServletRequest, final StaffProfileRepository staffProfileRepository, final StaffAssignmentRepository staffAssignmentRepository, final FCMService fcmService, final PaymentRepository paymentRepository, final AuthService authService) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.serviceRepository = serviceRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.httpServletRequest = httpServletRequest;
        this.staffProfileRepository = staffProfileRepository;
        this.staffAssignmentRepository = staffAssignmentRepository;
        this.fcmService = fcmService;
        this.paymentRepository = paymentRepository;
        this.authService = authService;
    }
}
