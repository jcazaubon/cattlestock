package com.kinetix.cattlestock.data;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByUsername(String username);

    boolean existsByUsername(String username);

    List<UserEntity> findAllByOrderByUsernameAsc();

    List<UserEntity> findByCompanyIdOrderByUsernameAsc(Long companyId);

    long countByCompanyId(Long companyId);
}
