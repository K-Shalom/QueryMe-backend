package com.year2.queryme.repository;

import com.year2.queryme.model.Guest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GuestRepository extends JpaRepository<Guest, Long> {
	Page<Guest> findAll(Pageable pageable);
}
