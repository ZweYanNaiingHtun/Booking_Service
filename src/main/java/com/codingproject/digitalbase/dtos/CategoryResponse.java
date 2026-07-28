package com.codingproject.digitalbase.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {

    private Long id;
    private String name;
    private boolean enabled;

    @JsonProperty("inPackage") // 🌟 Frontend ထံသို့ "inPackage": true/false ဟု တိကျစွာ ရောက်ရှိရန်
    private boolean inPackage;

}