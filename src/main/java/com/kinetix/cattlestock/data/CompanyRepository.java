package com.kinetix.cattlestock.data;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<CompanyEntity, Long> {

    List<CompanyEntity> findAllByOrderByNameAsc();
}
