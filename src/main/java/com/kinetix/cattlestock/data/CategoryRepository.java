package com.kinetix.cattlestock.data;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {

    /** El catalogo de la empresa, en el orden de la planilla. */
    List<CategoryEntity> findByCompanyIdOrderByPositionAscIdAsc(Long companyId);

    boolean existsByCompanyIdAndCode(Long companyId, String code);

    java.util.Optional<CategoryEntity> findByCompanyIdAndCode(Long companyId, String code);

    boolean existsByCompanyIdAndNameIgnoreCase(Long companyId, String name);

    long countByCompanyId(Long companyId);
}
