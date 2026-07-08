package com.codingproject.digitalbase.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RevenueBlock {
    private double totalRevenue;      // 💰 စုစုပေါင်း ဝင်ငွေ
    private double topServiceRevenue; // 💅 Top Service မှရသော ဝင်ငွေ
}