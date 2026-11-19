package com.kinetix.cattlestock.data;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EstablishmentRepository extends JpaRepository<EstablishmentEntity, String> {

    List<EstablishmentEntity> findAllByOrderByCodeAsc();

    List<EstablishmentEntity> findByCompanyIdOrderByCodeAsc(Long companyId);

    List<EstablishmentEntity> findByCompanyIdIsNull();

    long countByCompanyId(Long companyId);
}
