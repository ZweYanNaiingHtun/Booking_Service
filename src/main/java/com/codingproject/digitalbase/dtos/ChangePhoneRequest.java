package com.codingproject.digitalbase.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangePhoneRequest {

    @NotBlank(message = "Phone number cannot be empty")
    // 💡 မြန်မာဖုန်းနံပါတ် Format အတွက် Regular Expression (ဆရာကြီး စိတ်ကြိုက်ပြင်နိုင်ပါသည်)
    @Pattern(regexp = "^(09|\\+959)\\d{7,9}$", message = "Invalid phone number format")
    private String newPhone;
}