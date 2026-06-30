package com.codingproject.digitalbase.repository;

import com.codingproject.digitalbase.model.StaffProfile;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StaffProfileRepository extends JpaRepository<StaffProfile, Long> {

    // 🌟 နည်းလမ်း (၁) - အကြံပြုလိုသော မက်သတ် (Time Slot အလိုက် အားတဲ့သူကို တိုက်ရိုက်ရှာပေးခြင်း)
    // Service layer မှာ busyStaffIds တွေကို လိုက်ရှာပြီး List ထဲထည့်စရာ မလိုတော့ပါဘူးဗျာ။
    @Query("SELECT sp FROM StaffProfile sp WHERE sp.isAvailable = true AND sp.user.id NOT IN (" +
            "  SELECT sa.staffProfile.user.id FROM StaffAssignment sa " +
            "  WHERE sa.isBooked = true " +
            "  AND (:startTime < sa.endTime AND :endTime > sa.startTime)" +
            ")")
    List<StaffProfile> findAvailableStaffByTimeSlot(@Param("startTime") Instant startTime, @Param("endTime") Instant endTime);


    // 🌟 နည်းလမ်း (၂) - ဆရာကြီးရဲ့ မူလ ကုဒ်ကို Empty List သင့်လျော်အောင် ပြုပြင်ထားခြင်း
    // busyStaffIds က အားနေရင် (သို့) Null ဖြစ်ရင်လည်း Staff အားလုံးကို မှန်မှန်ကန်ကန် ပြန်ပေးပါလိမ့်မယ်။
    @Query("SELECT sp FROM StaffProfile sp WHERE sp.isAvailable = true " +
            "AND (COALESCE(:busyStaffIds) IS NULL OR sp.user.id NOT IN :busyStaffIds)")
    List<StaffProfile> findAvailableStaff(@Param("busyStaffIds") List<Long> busyStaffIds);


    List<StaffProfile> findByIsAvailableTrue();

    Optional<StaffProfile> findByUserId(Long userId);
}