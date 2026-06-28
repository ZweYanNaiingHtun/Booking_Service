package com.codingproject.digitalbase.dtos;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryWithServicesResponse {

    private Long id;
    private String name;
    private List<ServiceResponse> services;
}