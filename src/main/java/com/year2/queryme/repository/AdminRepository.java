package com.year2.queryme.repository;

import com.year2.queryme.model.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminRepository extends JpaRepository<Admin, Long> {
    boolean existsBySuperAdminTrue();
    Page<Admin> findAll(Pageable pageable);
}
