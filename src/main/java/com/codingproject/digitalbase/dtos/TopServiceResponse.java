package com.codingproject.digitalbase.dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TopServiceResponse {
    private String serviceName;          // ဝန်ဆောင်မှုအမည်
    private int appointment;            // Normal Appointment အရေအတွက်
    private int walkIn;                 // Walk-in Appointment အရေအတွက်
    private double price;               // ဝန်ဆောင်မှု တစ်ခုချင်းစီ၏ ဈေးနှုန်း
    private double revenue;             // အဆိုပါ Service မှ ရရှိသော စုစုပေါင်း ဝင်ငွေ (Total Revenue)
    private int totalCount;             // Appointment + Walk-in ပေါင်းကတ် (Total Booked Count)
    private double revenuePercentage;
    private double countPercentage;     // တစ်လစာ စုစုပေါင်းဝင်ငွေအပေါ် ၎င်း Service ၏ ဝင်ငွေရာခိုင်နှုန်း
}