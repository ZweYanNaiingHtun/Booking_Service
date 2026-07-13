//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.codingproject.digitalbase.repository;

import com.codingproject.digitalbase.model.Review;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    boolean existsByBookingId(Long bookingId);

    List<Review> findByStaffProfileId(Long staffProfileId);

    Page<Review> findByStaffProfileId(Long staffProfileId, Pageable pageable);

    List<Review> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<Review> findByStaffProfileIdOrderByCreatedAtDesc(Long staffProfileId);
}
