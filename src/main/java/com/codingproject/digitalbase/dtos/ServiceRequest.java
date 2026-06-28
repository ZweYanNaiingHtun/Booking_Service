package com.codingproject.digitalbase.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRequest {

    @NotBlank(message = "Service name is required")
    private String name;

    private String description;

    @NotNull(message = "Price is required")
    @Min(value = 0L, message = "Price must be greater than or equal to 0")
    private BigDecimal price;

    @NotNull(message = "Choose the package or not!")
    private boolean isPackage;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    @NotNull(message = "Duration in minutes is required")
    @Min(value = 5L, message = "Duration must be at least 5 minutes")
    private Integer durationInMinutes;
}