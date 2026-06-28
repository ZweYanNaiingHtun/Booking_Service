package com.codingproject.digitalbase.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffCreateRequest {

    @NotBlank(message = "Staff's Full name is required")
    private String fullName;

    @NotBlank(message = "Staff's Email Address is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Staff's Phone no is required")
    private String phoneNumber;

    @NotNull(message = "Staff's Date of Birth is required")
    private Instant dateOfBirth;

    private List<Long> specializedServiceIds;
}