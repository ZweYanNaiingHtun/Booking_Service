package com.codingproject.digitalbase.dtos;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class PackageRequest {
    private String name;
    private BigDecimal price;       // Admin သတ်မှတ်မည့် Package သီးသန့်ပရိုမိုးရှင်းဈေး
    private Long categoryId;    // Package များအတွက် သီးသန့်ဆောက်ထားသော Category ID
    private List<Long> serviceIds; // တွဲဖက်မည့် Service ID များ (ဥပမာ - [1, 2, 3])
}