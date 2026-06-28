package com.codingproject.digitalbase.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteProfileRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Phone number is required!")
    private String phoneNumber;

    @NotBlank(message = "Select your gender")
    private String gender;

    private MultipartFile profileImage;
}