package com.kinetix.cattlestock.data;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OpeningRepository extends JpaRepository<OpeningEntity, Long> {

    List<OpeningEntity> findByEstablishmentCodeIn(Collection<String> codes);

    Optional<OpeningEntity> findByEstablishmentCodeAndCategoryCode(String establishmentCode, String categoryCode);
}
