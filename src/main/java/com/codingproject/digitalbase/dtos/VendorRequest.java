package com.codingproject.digitalbase.dtos;

import lombok.*;
import org.springframework.web.multipart.MultipartFile; // 🌟
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorRequest {
    private String name;
    private String logo;
    private String bannerTitle;
    private String bannerDescription;
    private Instant eventStartDate;
    private Instant eventEndDate;

    // 🌟 Null စစ်နိုင်ရန် Wrapper Class (Boolean) သုံးပြီး Default True ပေးရန် ပြင်ဆင်ပါသည်
    private Boolean eventActive;

    // 🌟 Postman မှ File ဆွဲတင်မှုကို လက်ခံမည့် Field
    private MultipartFile bannerFile;
}