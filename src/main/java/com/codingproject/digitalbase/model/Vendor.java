package com.codingproject.digitalbase.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "vendors")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Vendor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String logo;

    private String bannerTitle;
    private String bannerDescription;
    private String bannerImage; // ဒေတာဘေ့စ်ထဲတွင် ပုံရဲ့ File Name (သို့) URL သာ သိမ်းမည်

    private Instant eventStartDate;
    private Instant eventEndDate;

    // 🌟 နာမည်အား eventActive ဟု ပြောင်းလဲပြီး Jackson မျက်စိမလည်အောင် ထိန်းလိုက်ပါသည်
    private boolean eventActive;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}