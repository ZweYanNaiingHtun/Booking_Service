package com.codingproject.digitalbase.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    @NotBlank(message = "Full name cannot be empty")
    private String fullName;

    @NotBlank(message = "Phone number cannot be empty!")
    private String phone;

    private String gender;

    private MultipartFile profileImage;
}