//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.codingproject.digitalbase.service;

import com.codingproject.digitalbase.dtos.ServiceRequest;
import com.codingproject.digitalbase.dtos.ServiceResponse;
import java.util.List;

public interface BusinessServiceService {
    List<ServiceResponse> getAllServices();

    ServiceResponse getServiceById(Long id);

    ServiceResponse createService(ServiceRequest request);

    ServiceResponse updateService(Long id, ServiceRequest request);

    void deleteService(Long id);

    void restoreService(Long id);
}
