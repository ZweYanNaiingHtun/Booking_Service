package com.codingproject.digitalbase.dtos;

import lombok.*;

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
    private String createdAt;
}