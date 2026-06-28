package com.codingproject.digitalbase.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "staff_assignments")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class StaffAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_profile_id", nullable = false)
    private StaffProfile staffProfile;

    // 🌟 LocalDate/LocalTime အစား Instant သို့ ပြောင်းလဲခြင်း
    @Column(name = "start_time", nullable = false)
    private Instant startTime; // Slot စတင်မည့် အချိန် (ဥပမာ - Walk-In ဝင်လာသည့်အချိန်)

    @Column(name = "end_time", nullable = false)
    private Instant endTime;   // Slot ပြီးဆုံးမည့် အချိန်

    @Column(name = "is_booked", nullable = false)
    private boolean isBooked = false;

    @OneToOne(mappedBy = "staffAssignment", fetch = FetchType.LAZY)
    private Booking booking;
}