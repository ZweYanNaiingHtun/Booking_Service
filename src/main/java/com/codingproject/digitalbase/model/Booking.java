package com.codingproject.digitalbase.model;

import com.codingproject.digitalbase.enums.BookingStatus;

import jakarta.persistence.*;

import jakarta.validation.constraints.Future;

import jakarta.validation.constraints.NotNull;

import lombok.*;



import java.time.Instant;





@Entity

@Table(name = "bookings")

@Getter @Setter

@NoArgsConstructor @AllArgsConstructor

public class Booking {

    @Id

    @GeneratedValue(strategy = GenerationType.IDENTITY)

    private Long id;



    @ManyToOne(fetch = FetchType.LAZY)

    @JoinColumn(name = "customer_id", nullable = false)

    private User customer; // မည်သည့် Customer မှာယူသည်ကို ချိတ်ဆက်ခြင်း [cite: 24, 34]



    @ManyToOne(fetch = FetchType.LAZY)

    @JoinColumn(name = "service_id", nullable = false)

    private BusinessService businessService; // မည်သည့် ဝန်ဆောင်မှုကို ယူသည်ကို ချိတ်ဆက်ခြင်း



    @Column(nullable = false)

    private Instant bookingDate; // ဘွတ်ကင်လုပ်မည့် နေ့ရက်နှင့် အချိန်



    private String notes; // မှတ်စုတိုများ



    @Enumerated(EnumType.STRING)

    @Column(name = "status", length = 15)

    private BookingStatus status = BookingStatus.PENDING; // စတင်တင်ချင်း အခြေအနေသည် Pending ဖြစ်မည်



    private Instant createdAt = Instant.now();



    @ManyToOne(fetch = FetchType.LAZY)

    @JoinColumn(name = "created_by_id", nullable = false)

    private User createdBy; // 🌟 ဘွတ်ကင်ကို တကယ်လုပ်ပေးလိုက်သူ (Staff သို့မဟုတ် Customer ကိုယ်တိုင်)



    @ManyToOne(fetch = FetchType.LAZY)

    @JoinColumn(name = "requested_staff_profile_id") // DB Column Name ပြောင်းလဲမည်

    private StaffProfile requestedStaff;



    @ManyToOne(fetch = FetchType.LAZY)

    @JoinColumn(name = "assigned_staff_profile_id") // DB Column Name ပြောင်းလဲမည်

    private StaffProfile assignedStaff;



    @OneToOne(fetch = FetchType.LAZY,cascade = CascadeType.ALL)

    @JoinColumn(name = "staff_assignment_id")

    private StaffAssignment staffAssignment;// 🌟 ဤ Booking သည် မည်သည့် Staff ရဲ့ ဘယ်အချိန် Slot ကို ယူထားခြင်းဖြစ်ကြောင်း ညွှန်းဆိုခြင်း



    @OneToOne(mappedBy = "booking", cascade = CascadeType.ALL, fetch = FetchType.LAZY)

    private Review review;



    @OneToOne(mappedBy = "booking", cascade = CascadeType.ALL, fetch = FetchType.LAZY)

    private Payment payment;



    private String cancelledBy;





}