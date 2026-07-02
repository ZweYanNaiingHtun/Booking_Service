package com.codingproject.digitalbase.model;



import jakarta.persistence.*;

import lombok.*;

import java.math.BigDecimal;

import java.time.Instant;



@Entity

@Table(name = "payments")

@Getter @Setter

@NoArgsConstructor @AllArgsConstructor

@Builder

public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    private BigDecimal baseAmount;// 🌟 Normal Price အတွက်

    private BigDecimal extraAmount; // 🌟 Extra Price အတွက်

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount; // ငွေပမာဏ

    @Column(nullable = false, length = 20)
    private String status; // e.g., PENDING, COMPLETED, FAILED

    private String invoiceNumber;



    private String paymentMethod;// Invoice နံပါတ်



    private Instant paymentDate;



    private Instant createdAt = Instant.now();



    public BigDecimal calculateTotalAmount() {

        BigDecimal base = this.baseAmount != null ? this.baseAmount : BigDecimal.ZERO;

        BigDecimal extra = this.extraAmount != null ? this.extraAmount : BigDecimal.ZERO;

        return base.add(extra);

    }
}
