package com.codingproject.digitalbase.dtos;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffResponse {

    private Long id;
    private String fullName;
    private String code;
    private String email;
    private String phone;
    private String gender;
    private String profilePicture;
    private boolean enabled;
    private boolean isAvailable;
    private String createdAt;
    private String dateOfBirth;
    private List<Long> specializedServiceIds;
}