package com.codingproject.digitalbase.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor  // 🌟 Jackson ဒေတာပြောင်းလဲနိုင်ဖို့ ဒါက မရှိမဖြစ် လိုအပ်ပါတယ်!
@AllArgsConstructor
public class RefreshTokenRequest {
    private String refreshToken;
}