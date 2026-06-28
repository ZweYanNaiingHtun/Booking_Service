package com.codingproject.digitalbase.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffAssignmentRequest {

    @NotNull(message = "Staff ID is required")
    private Long staffId;
}