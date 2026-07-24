package com.codingproject.digitalbase.repository;

import com.codingproject.digitalbase.dtos.CustomerBookingResponse;
import com.codingproject.digitalbase.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    int deleteByEnabledFalseAndCreatedAtBefore(Instant timeLimit);
    boolean existsByEmail(String superAdminEmail);
    Optional<User> findByPhone(@NotBlank(message = "Customer phone number is required for walk-in booking") String customerPhone);
    boolean existsByPhone(String phone);

    @Query(value = "SELECT MAX(code) FROM users WHERE code REGEXP CONCAT('^', :prefix, '[0-9]+$')", nativeQuery = true)
    String findMaxCodeByPrefix(@Param("prefix") String prefix);

    Optional<User> findByCode(String code);

    // 🌟 [FIXED] u.id ကို u.Id (I အကြီး) သို့ ပြောင်းလဲ၍ Error အား ရှင်းလင်းထားပါသည်
    @Query("SELECT new com.codingproject.digitalbase.dtos.CustomerBookingResponse(" +
            "u.Id, u.fullName, u.email, u.phone, u.gender, u.enabled, u.profilePicture, COUNT(b)) " +
            "FROM User u " +
            "LEFT JOIN u.bookings b " +
            "JOIN u.roles r " +
            "WHERE r.role = com.codingproject.digitalbase.enums.RoleName.CUSTOMER " +
            "GROUP BY u.Id, u.fullName, u.email, u.phone, u.gender, u.enabled, u.profilePicture")
    List<CustomerBookingResponse> findAllCustomersWithBookingCount();

    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.role = 'CUSTOMER'")
    long countTotalCustomers();

    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.role = 'CUSTOMER' AND u.enabled = false")
    long countBlockedCustomers();

    // 🌟 1. Active Customer များအတွက် (Enabled = true + Role = CUSTOMER + ID/Email/Phone/Name Search)
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.role = com.codingproject.digitalbase.enums.RoleName.CUSTOMER " +
            "AND u.enabled = true " +
            "AND (:search IS NULL OR TRIM(:search) = '' OR " +
            "     LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "     LOWER(u.phone) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "     LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "     LOWER(u.code) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> searchCustomers(@Param("search") String search, Pageable pageable);


    // 🌟 2. Blocked Customer များအတွက် (Enabled = false + Role = CUSTOMER + ID/Email/Phone/Name Search)
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.role = com.codingproject.digitalbase.enums.RoleName.CUSTOMER " +
            "AND u.enabled = false " +
            "AND (:search IS NULL OR TRIM(:search) = '' OR " +
            "     LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "     LOWER(u.phone) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "     LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "     LOWER(u.code) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> searchBlockedCustomers(@Param("search") String search, Pageable pageable);

    Optional<User> findByFcmToken(String fcmToken);

    // 🌟 1. Active Customers များကို Name, Phone, Email, Customer Code တို့ဖြင့် ရှာဖွေခြင်
}