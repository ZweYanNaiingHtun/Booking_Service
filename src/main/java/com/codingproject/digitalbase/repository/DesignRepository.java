package com.codingproject.digitalbase.repository;

import com.codingproject.digitalbase.model.Design;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DesignRepository extends JpaRepository<Design, Long> {

    // 🌟 React အများဆုံးကနေ စီမယ်၊ တူရင် Title အလိုက် A to Z စီမယ်
    List<Design> findAllByOrderByReactionCountDescTitleAsc();
}
