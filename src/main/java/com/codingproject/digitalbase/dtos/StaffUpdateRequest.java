package com.codingproject.digitalbase.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.time.Instant; // သို့မဟုတ် ဆရာကြီး သုံးထားသော Date Type (LocalDate)

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffUpdateRequest {

    @NotBlank(message = "Full name cannot be empty")
    private String fullName;

    @NotBlank(message = "Email cannot be empty")
    private String email;

    @NotBlank(message = "Phone number cannot be empty")
    private String phoneNumber;

    private Instant dateOfBirth; // 🌟 ပြင်ဆင်မည့် မွေးနေ့
}