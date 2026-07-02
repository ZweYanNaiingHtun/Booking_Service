//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.codingproject.digitalbase.service;

import com.codingproject.digitalbase.dtos.StaffCreateRequest;
import com.codingproject.digitalbase.dtos.StaffResponse;
import com.codingproject.digitalbase.dtos.StaffUpdateRequest;
import java.util.List;

public interface StaffManagementService {
    StaffResponse createStaffUser(StaffCreateRequest request);

    StaffResponse linkStaffWithServices(Long staffId, List<Long> serviceIds);

    List<StaffResponse> getAllStaffs();

    StaffResponse updateStaffUser(Long staffId, StaffUpdateRequest request);

    StaffResponse toggleStaffStatus(Long staffId, boolean enable);

    StaffResponse toggleStaffAvailability(Long staffProfileId, boolean available);
}
