package com.kinetix.cattlestock.data;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MovementRepository extends JpaRepository<MovementEntity, Long> {

    /** Movimientos que tocan alguno de los establecimientos, por origen o por destino. */
    List<MovementEntity> findByEstablishmentCodeInOrDestEstablishmentCodeInOrderByDateAscIdAsc(
            Collection<String> codes, Collection<String> destCodes);

    /** Los movimientos de esos establecimientos en un rango de fechas, del mas nuevo al mas viejo. */
    List<MovementEntity> findByEstablishmentCodeInAndDateBetweenOrderByDateDescIdDesc(
            Collection<String> establishmentCodes, java.time.LocalDate from, java.time.LocalDate to);

    /** Si la categoria tuvo algun movimiento, como origen o como destino, en esos establecimientos. */
    boolean existsByCategoryCodeAndEstablishmentCodeIn(String categoryCode, Collection<String> establishmentCodes);

    /**
     * Si la categoria fue destino de un cambio de categoria (que ocurre dentro del establecimiento de origen y
     * no lleva establecimiento de destino) o de un traslado (entre establecimientos de la misma empresa).
     */
    boolean existsByDestCategoryCodeAndEstablishmentCodeIn(String categoryCode, Collection<String> establishmentCodes);
}
