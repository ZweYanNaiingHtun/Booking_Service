package com.codingproject.digitalbase.dtos;

import lombok.Data;

@Data
public class FcmTokenRequest {
    private String token; // JSON Body ထဲမှ {"token": "..."} အား ဖတ်ယူရန်
}