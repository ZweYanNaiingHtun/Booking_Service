package com.codingproject.digitalbase.dtos;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DesignResponseDto {
    private Long id;
    private String title;
    private String imageUrl;
    private int reactionCount;
    private boolean isFavorited; // 🌟 လက်ရှိ Login ဝင်ထားသူက Like ပေးထားလား (True/False)
}