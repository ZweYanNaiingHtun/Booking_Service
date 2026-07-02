package com.codingproject.digitalbase.dtos;

import lombok.*;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffPerformance {

    // ======== ေနဂိုရှိပြီးသား Core Columns ========
    private Long staffId;
    private String staffName;
    private Long completedJobsCount; // Services Completed အဖြစ်ရော Count အဖြစ်ပါ သုံးပါမည်
    private Double ratingAverage;     // Client Rating အဖြစ် သုံးပါမည်

    // ======== 🌟 image_616ad1.png (Staffs Management) အတွက် ထပ်တိုးကော်လံများ ========
    private String staffCode;         // e.g., "St-001"
    private String staffRole;         // e.g., "Nail Artist"
    private String profileImage;      // Profile Picture URL/Path
    private String phoneNumber;       // Contact (Phone)
    private String email;             // Contact (Email)
    private LocalDate dateOfBirth;    // Date of Birth (27/9/2000)
    private LocalDate joinedDate;     // Joined Date (1/1/2026)
    private String status;            // e.g., "Available", "In Progress", "Unavailable"

    // ======== 🌟 Staff Management_2.png (Nail Artist Performance) အတွက် ထပ်တိုးကော်လံများ ========
    private Double totalRevenue;      // Revenue (e.g., 1000000.0)
    private Double totalCommission;   // Commission (e.g., 200000.0)

    // 🌟 Database Native Projection Mapping အတွက် Object Constructor ကို နဂိုအတိုင်း ထည့်သွင်းထားပါတယ်
    // (ကျန်တဲ့ Field အသစ်တွေကို Null သို့မဟုတ် Default Value သတ်မှတ်ပေးထားလို့ Query Run ရင် Error တက်မှာ မဟုတ်တော့ပါဘူး)
    public StaffPerformance(Object staffId, Object staffName, Object completedJobsCount, Object ratingAverage) {
        this.staffId = staffId != null ? Long.valueOf(staffId.toString()) : null;
        this.staffName = staffName != null ? staffName.toString() : null;
        this.completedJobsCount = completedJobsCount != null ? Long.valueOf(completedJobsCount.toString()) : 0L;
        this.ratingAverage = ratingAverage != null ? Double.valueOf(ratingAverage.toString()) : 0.0;

        // Field အသစ်များကို Default ချထားခြင်း
        this.staffCode = null;
        this.staffRole = null;
        this.profileImage = null;
        this.phoneNumber = null;
        this.email = null;
        this.dateOfBirth = null;
        this.joinedDate = null;
        this.status = null;
        this.totalRevenue = 0.0;
        this.totalCommission = 0.0;
    }
}