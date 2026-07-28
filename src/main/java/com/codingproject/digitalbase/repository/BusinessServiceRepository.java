//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.codingproject.digitalbase.repository;

import com.codingproject.digitalbase.model.BusinessService;
import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BusinessServiceRepository extends JpaRepository<BusinessService, Long> {
    // 🌟 1. Target Service သည် မည်သည့် Package ၏ bundledServices ထဲတွင်မဆို ပါဝင်နေခြင်း ရှိ/မရှိ စစ်ဆေးခြင်း
    @Query("SELECT COUNT(p) > 0 FROM BusinessService p JOIN p.bundledServices s WHERE s.id = :serviceId")
    boolean isServiceBundledInAnyPackage(@Param("serviceId") Long serviceId);

    // 🌟 2. Target Category အောက်ရှိ Service တစ်ခုခုသည် Package ၏ bundledServices ထဲတွင် ပါဝင်နေခြင်း ရှိ/မရှိ စစ်ဆေးခြင်း
    @Query("SELECT COUNT(p) > 0 FROM BusinessService p JOIN p.bundledServices s WHERE s.category.id = :categoryId")
    boolean isCategoryServiceBundledInAnyPackage(@Param("categoryId") Long categoryId);

    boolean existsByName(String name);

    // 🌟 Package ထဲတွင် ပါဝင်နေသော Category ID များကို သီးသန့် DISTINCT ထုတ်ယူခြင်း
    @Query("SELECT DISTINCT s.category.id FROM BusinessService p JOIN p.bundledServices s")
    Set<Long> findCategoryIdsInPackages();

    // 🌟 1. Category အောက်တွင် active ဖြစ်နေသော (isEnabled = true) service ကျန်မကျန် စစ်ဆေးခြင်း
    boolean existsByCategoryIdAndIsEnabledTrue(Long categoryId);
}
