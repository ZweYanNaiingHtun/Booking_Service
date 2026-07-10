package com.codingproject.digitalbase.model;



import jakarta.persistence.*;

import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;


import java.math.BigDecimal;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;


@Builder
@Entity
@Table(name = "services")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BusinessService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Lob
    @JdbcTypeCode(Types.LONGVARCHAR)
    private String description;

    @Column(nullable = false)
    private BigDecimal price; // ဈေးနှုန်း

    @Column(nullable = false)
    private boolean is_package;

    @Builder.Default // ⚠️ အရေးကြီး: Lombok Builder သုံးရင် ဒါလေးမပါရင် Default တန်ဖိုးကို false အဖြစ်ပဲ ဆောက်သွားမှာပါ
    @Column(name = "enabled", nullable = false)
    private boolean isEnabled = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "duration_in_minutes", nullable = false)
    private Integer durationInMinutes;

    // BusinessService.java မော်ဒယ်ထဲတွင် သွားရောက်ဖြည့်စွက်ပေးရန်
    @ManyToMany
    @JoinTable(
            name = "package_services",
            joinColumns = @JoinColumn(name = "package_id"),
            inverseJoinColumns = @JoinColumn(name = "service_id")
    )
    private List<BusinessService> bundledServices = new ArrayList<>();

}