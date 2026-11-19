package com.kinetix.cattlestock.data;

import java.time.LocalDate;

import com.kinetix.cattlestock.domain.MovementType;
import jakarta.persistence.Column;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Un movimiento como lo carga el usuario. Traslado y cambio de categoria se guardan como
 * una sola fila con dos lados, igual que en el dominio.
 */
@Entity
@Table(name = "movement",
        uniqueConstraints = @UniqueConstraint(columnNames = "business_id"),
        indexes = {
                @Index(name = "idx_movement_establishment", columnList = "establishment_code"),
                @Index(name = "idx_movement_dest", columnList = "dest_establishment_code")})
public class MovementEntity {

    @Id
    @GeneratedValue
    private Long id;

    private String businessId;

    private LocalDate date;

    // VARCHAR y no ENUM: agregar un valor al enum de Java no obliga a alterar la base
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 30)
    private MovementType type;

    private String establishmentCode;

    private String categoryCode;

    private int quantity;

    private String destEstablishmentCode;

    private String destCategoryCode;

    private Integer cueros;

    private String note;

    /** Borrado logico: la fila queda para la historia, pero no cuenta en el stock. Null en las filas viejas: no borrada. */
    private Boolean deleted = Boolean.FALSE;

    private String deletedBy;

    private java.time.LocalDateTime deletedAt;

    private String modifiedBy;

    private java.time.LocalDateTime modifiedAt;

    protected MovementEntity() { }

    public MovementEntity(String businessId, LocalDate date, MovementType type, String establishmentCode,
                          String categoryCode, int quantity, String destEstablishmentCode, String destCategoryCode,
                          Integer cueros, String note) {
        this.businessId = businessId;
        this.date = date;
        this.type = type;
        this.establishmentCode = establishmentCode;
        this.categoryCode = categoryCode;
        this.quantity = quantity;
        this.destEstablishmentCode = destEstablishmentCode;
        this.destCategoryCode = destCategoryCode;
        this.cueros = cueros;
        this.note = note;
    }

    public Long getId() { return id; }
    public String getBusinessId() { return businessId; }
    public LocalDate getDate() { return date; }
    public MovementType getType() { return type; }
    public String getEstablishmentCode() { return establishmentCode; }
    public String getCategoryCode() { return categoryCode; }
    public int getQuantity() { return quantity; }
    public String getDestEstablishmentCode() { return destEstablishmentCode; }
    public String getDestCategoryCode() { return destCategoryCode; }
    public Integer getCueros() { return cueros; }
    public String getNote() { return note; }
    public boolean isDeleted() { return deleted != null && deleted; }
    public String getDeletedBy() { return deletedBy; }
    public java.time.LocalDateTime getDeletedAt() { return deletedAt; }
    public String getModifiedBy() { return modifiedBy; }
    public java.time.LocalDateTime getModifiedAt() { return modifiedAt; }

    /** Cambia lo que se puede corregir de un movimiento: el tipo, el establecimiento y el destino no cambian. */
    public void applyChanges(LocalDate date, String categoryCode, int quantity, String destCategoryCode,
                             Integer cueros, String note, String user, java.time.LocalDateTime when) {
        this.date = date;
        this.categoryCode = categoryCode;
        this.quantity = quantity;
        this.destCategoryCode = destCategoryCode;
        this.cueros = cueros;
        this.note = note;
        this.modifiedBy = user;
        this.modifiedAt = when;
    }

    public void markDeleted(String user, java.time.LocalDateTime when) {
        this.deleted = Boolean.TRUE;
        this.deletedBy = user;
        this.deletedAt = when;
    }

    public void restore() {
        this.deleted = Boolean.FALSE;
        this.deletedBy = null;
        this.deletedAt = null;
    }
}
