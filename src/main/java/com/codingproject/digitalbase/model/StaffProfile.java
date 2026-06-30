package com.codingproject.digitalbase.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "staff_profiles")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class StaffProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🌟 User နှင့် One-to-One ချိတ်ဆက်ခြင်း (ဝန်ထမ်းတစ်ယောက်တွင် Profile တစ်ခုသာ ရှိမည်)
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "staff_specializations", // တည်ဆောက်မည့် Junction Table နာမည်
            joinColumns = @JoinColumn(name = "staff_profile_id"),
            inverseJoinColumns = @JoinColumn(name = "service_id")
    )
    private Set<BusinessService> specializedServices = new HashSet<>();

    private String specializedName;

    @Column(name = "is_available", nullable = false)
    private boolean isAvailable = true; // လက်ရှိ Booking လက်ခံနိုင်ခြင်း ရှိ/မရှိ

    @Column(name = "rating")
    private Double rating = 0.0; // ဝန်ထမ်း၏ Rating (Optional)

    private Instant joinedAt;

    @OneToMany(mappedBy = "staffProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StaffAssignment> assignments = new ArrayList<>();

    // 🌟 ဖြည့်စွက်ချက်အသစ်: ဤဝန်ထမ်း လက်ခံရရှိထားသော ဘွတ်ကင်များအား ပြန်လည်ကြည့်ရှုနိုင်ရန်
    @OneToMany(mappedBy = "requestedStaff")
    private List<Booking> requestedBookings = new ArrayList<>();

    @OneToMany(mappedBy = "assignedStaff")
    private List<Booking> assignedBookings = new ArrayList<>();

    @OneToMany(mappedBy = "staffProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Review> reviews = new ArrayList<>();
}