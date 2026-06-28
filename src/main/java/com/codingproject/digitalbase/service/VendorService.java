//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.codingproject.digitalbase.service;

import com.codingproject.digitalbase.dtos.VendorRequest;
import com.codingproject.digitalbase.dtos.VendorResponse;
import java.util.List;

public interface VendorService {
    VendorResponse createVendor(VendorRequest request);

    List<VendorResponse> getAllVendors();

    VendorResponse getVendorDetails(Long id);

    VendorResponse updateVendorDetails(Long id, VendorRequest request);

    void deleteVendor(Long id);
}
