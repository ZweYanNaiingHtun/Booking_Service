package com.codingproject.digitalbase.model;

import com.codingproject.digitalbase.enums.LeaveType;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant; // 🌟 Instant သို့ ပြောင်းလဲရန်

@Entity
@Table(name = "staff_leaves")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffLeave {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_profile_id", nullable = false)
    private StaffProfile staffProfile;

    @Enumerated(EnumType.STRING)
    @Column(name = "leave_type", nullable = false)
    private LeaveType leaveType;

    @Column(name = "start_date", nullable = false)
    private Instant startDate; // 🌟 LocalDate မှ Instant သို့ ပြောင်းလဲခြင်း

    @Column(name = "end_date", nullable = false)
    private Instant endDate; // 🌟 LocalDate မှ Instant သို့ ပြောင်းလဲခြင်း

    private String note;
}