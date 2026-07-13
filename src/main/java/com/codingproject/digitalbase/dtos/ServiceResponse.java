package com.codingproject.digitalbase.dtos;

import java.math.BigDecimal;
import java.util.List;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceResponse {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private boolean isPackage;
    private boolean isEnabled;
    private Long categoryId;
    private String categoryName;
    private Integer durationInMinutes;
    private List<String> includedServices;
}