package com.codingproject.digitalbase.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "designs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Design {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;      // ပုံရဲ့ နာမည် (A-Z စီဖို့ သုံးမယ်)

    @Column(name = "image_url")
    private String imageUrl;   // ပုံရဲ့ URL

    @Column(name = "reaction_count")
    private int reactionCount = 0; // Sorting အတွက် Like အရေအတွက်ကို သိမ်းထားမယ့်နေရာ
}
