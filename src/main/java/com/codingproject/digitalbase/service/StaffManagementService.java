//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.codingproject.digitalbase.service;

import com.codingproject.digitalbase.dtos.*;

import java.time.Instant;
import java.util.List;

public interface StaffManagementService {
    StaffResponse createStaffUser(StaffCreateRequest request);

//    StaffResponse linkStaffWithServices(Long staffId, List<Long> serviceIds);

    List<StaffResponse> getAllStaffs();

    void assignStaffLeave(StaffLeaveRequest request);

    StaffResponse updateStaffUser(Long staffId, StaffUpdateRequest request);

    List<StaffLeaveDetailResponse> getStaffLeavesBySelectedDate(Instant targetDate);

    StaffResponse toggleStaffStatus(Long staffId, boolean enable);

    DailyStaffStatusResponse getDailyStaffStatus(Instant targetDate);

    // 🎯 ပြက္ခဒိန်အတွက် Range အလိုက် Loop ပတ်ပြီး Data ထုတ်ပေးခြင်း
    List<CalendarMonthResponse> getCalendarMonthOverview(Integer year, Integer month, Long staffId);

    StaffResponse toggleStaffAvailability(Long staffProfileId, boolean available);

}
