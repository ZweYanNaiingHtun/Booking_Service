package com.codingproject.digitalbase.repository;

import com.codingproject.digitalbase.enums.RoleName;
import com.codingproject.digitalbase.model.Role;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByRole(RoleName role);
}
