package com.codingproject.digitalbase.dtos;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class PackageResponse {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer durationInMinutes; // အလိုအလျောက် ပေါင်းပေးထားသော မိနစ်စုစုပေါင်း
    private boolean isPackage;
    private boolean isEnabled;
    private Long categoryId;
    private String categoryName;
    private List<String> includedServices; // ဘာ Service တွေပါလဲဆိုတာ UI မှာ ပြရန်
}