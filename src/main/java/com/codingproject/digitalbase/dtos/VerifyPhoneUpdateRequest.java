package com.codingproject.digitalbase.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyPhoneUpdateRequest {

    @NotBlank(message = "OTP code is required")
    private String otp;

    @NotBlank(message = "New phone number is required")
    private String newPhoneNumber;
}