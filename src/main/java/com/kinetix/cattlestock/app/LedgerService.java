package com.kinetix.cattlestock.app;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kinetix.cattlestock.data.CategoryEntity;
import com.kinetix.cattlestock.data.CategoryRepository;
import com.kinetix.cattlestock.data.CompanyEntity;
import com.kinetix.cattlestock.data.CompanyRepository;
import com.kinetix.cattlestock.data.EstablishmentEntity;
import com.kinetix.cattlestock.data.EstablishmentRepository;
import com.kinetix.cattlestock.data.MonthCloseEntity;
import com.kinetix.cattlestock.data.MonthCloseRepository;
import com.kinetix.cattlestock.data.MovementEntity;
import com.kinetix.cattlestock.data.MovementRepository;
import com.kinetix.cattlestock.data.OpeningEntity;
import com.kinetix.cattlestock.data.OpeningRepository;
import com.kinetix.cattlestock.domain.DomainException;
import com.kinetix.cattlestock.domain.Ledger;
import com.kinetix.cattlestock.domain.Movement;
import com.kinetix.cattlestock.domain.Planilla;
import com.kinetix.cattlestock.domain.Species;

/**
 * Capa de aplicacion: arma un Ledger desde la base y delega todas las reglas en el dominio.
 *
 * El armado carga la configuracion completa (establecimientos, categorias, stocks iniciales
 * y cierres, que son tablas chicas) y los movimientos del componente conectado del
 * establecimiento pedido: si hay traslados, se expande hasta que las referencias y los
 * saldos de cada clave cierren igual que en la realidad.
 */
@Service
public class LedgerService {

    private final CompanyRepository companies;
    private final EstablishmentRepository establishments;
    private final CategoryRepository categories;
    private final OpeningRepository openings;
    private final MovementRepository movements;
    private final MonthCloseRepository closes;

    public LedgerService(CompanyRepository companies, EstablishmentRepository establishments,
                         CategoryRepository categories, OpeningRepository openings,
                         MovementRepository movements, MonthCloseRepository closes) {
        this.companies = companies;
        this.establishments = establishments;
        this.categories = categories;
        this.openings = openings;
        this.movements = movements;
        this.closes = closes;
    }

    // --- Configuracion ---

    @Transactional
    public void configureCompany(String name, YearMonth startMonth) {
        if (companies.findById(CompanyEntity.ID).isPresent()) {
            throw new DomainException("La empresa ya esta configurada");
        }
        companies.save(new CompanyEntity(name, startMonth.toString()));
    }

    @Transactional
    public void createEstablishment(String code, String name) {
        if (establishments.existsById(code)) {
            throw new DomainException("El establecimiento " + code + " ya existe");
        }
        establishments.save(new EstablishmentEntity(code, name));
    }

    @Transactional
    public void createCategory(String establishmentCode, String code, String name, Species species) {
        EstablishmentEntity est = requireEstablishment(establishmentCode);
        if (categories.existsByEstablishmentCodeAndCode(establishmentCode, code)) {
            throw new DomainException("La categoria " + code + " ya existe en " + est.getName());
        }
        int position = categories.findByEstablishmentCodeOrderByPositionAsc(establishmentCode).size();
        categories.save(new CategoryEntity(establishmentCode, code, name, species, position));
    }

    @Transactional
    public void setOpening(String establishmentCode, String categoryCode, int quantity) {
        Ledger ledger = ledgerFor(List.of(establishmentCode));
        ledger.setOpening(establishmentCode, categoryCode, quantity);
        OpeningEntity o = openings.findByEstablishmentCodeAndCategoryCode(establishmentCode, categoryCode)
                .orElseGet(() -> new OpeningEntity(establishmentCode, categoryCode));
        o.setQuantity(quantity);
        openings.save(o);
    }

    // --- Movimientos ---

    @Transactional
    public void addMovement(Movement m) {
        Set<String> codes = new LinkedHashSet<>();
        codes.add(m.establishmentId());
        if (m.destEstablishmentId() != null) codes.add(m.destEstablishmentId());
        ledgerFor(codes).add(m);
        movements.save(toEntity(m));
    }

    /** Agrega todos o ninguno. Sirve para la carga en grilla. */
    @Transactional
    public void addBatch(List<Movement> batch) {
        if (batch.isEmpty()) return;
        Set<String> codes = new LinkedHashSet<>();
        for (Movement m : batch) {
            codes.add(m.establishmentId());
            if (m.destEstablishmentId() != null) codes.add(m.destEstablishmentId());
        }
        ledgerFor(codes).addAll(batch);
        for (Movement m : batch) movements.save(toEntity(m));
    }

    // --- Consultas ---

    @Transactional(readOnly = true)
    public Planilla planilla(String establishmentCode, YearMonth month) {
        requireEstablishment(establishmentCode);
        return ledgerFor(List.of(establishmentCode)).planilla(establishmentCode, month);
    }

    // --- Cierre de mes ---

    @Transactional
    public void closeMonth(String establishmentCode, YearMonth month) {
        requireEstablishment(establishmentCode);
        ledgerFor(List.of(establishmentCode)).closeMonth(establishmentCode, month);
        MonthCloseEntity mc = closes.findById(establishmentCode)
                .orElseGet(() -> new MonthCloseEntity(establishmentCode, month.toString()));
        mc.setClosedThrough(month.toString());
        closes.save(mc);
    }

    @Transactional
    public void reopenLastMonth(String establishmentCode) {
        requireEstablishment(establishmentCode);
        CompanyEntity company = company();
        ledgerFor(List.of(establishmentCode)).reopenLastMonth(establishmentCode);
        MonthCloseEntity mc = closes.findById(establishmentCode)
                .orElseThrow(() -> new DomainException("No hay meses cerrados"));
        YearMonth through = YearMonth.parse(mc.getClosedThrough());
        if (through.toString().equals(company.getStartMonth())) {
            closes.delete(mc);
        } else {
            mc.setClosedThrough(through.minusMonths(1).toString());
            closes.save(mc);
        }
    }

    // --- Armado del Ledger desde la base ---

    private Ledger ledgerFor(Collection<String> requested) {
        CompanyEntity company = company();
        YearMonth startMonth = YearMonth.parse(company.getStartMonth());

        Ledger ledger = new Ledger(company.getName(), startMonth);

        for (EstablishmentEntity e : establishments.findAllByOrderByCodeAsc()) {
            ledger.addEstablishment(e.getCode(), e.getName());
        }
        for (CategoryEntity c : categories.findAllByOrderByEstablishmentCodeAscPositionAsc()) {
            ledger.addCategory(c.getEstablishmentCode(), c.getCode(), c.getName(), c.getSpecies());
        }
        for (OpeningEntity o : openings.findAll()) {
            ledger.setOpening(o.getEstablishmentCode(), o.getCategoryCode(), o.getQuantity());
        }

        // Movimientos del componente conectado: se expande por los traslados hasta cerrar.
        Set<String> scope = new LinkedHashSet<>(requested);
        List<MovementEntity> loaded;
        while (true) {
            loaded = movements.findByEstablishmentCodeInOrDestEstablishmentCodeInOrderByDateAscIdAsc(scope, scope);
            Set<String> next = new LinkedHashSet<>(scope);
            for (MovementEntity me : loaded) {
                if (me.getEstablishmentCode() != null) next.add(me.getEstablishmentCode());
                if (me.getDestEstablishmentCode() != null) next.add(me.getDestEstablishmentCode());
            }
            if (next.size() == scope.size()) break;
            scope = next;
        }
        for (MovementEntity me : loaded) {
            ledger.add(toDomain(me));
        }

        // Los cierres se rehacen en orden, a partir del estado persistido.
        for (MonthCloseEntity mc : closes.findAll()) {
            YearMonth target = YearMonth.parse(mc.getClosedThrough());
            YearMonth next = startMonth;
            while (!next.isAfter(target)) {
                ledger.closeMonth(mc.getEstablishmentCode(), next);
                next = next.plusMonths(1);
            }
        }
        return ledger;
    }

    private CompanyEntity company() {
        return companies.findById(CompanyEntity.ID)
                .orElseThrow(() -> new DomainException("La empresa no esta configurada"));
    }

    private EstablishmentEntity requireEstablishment(String code) {
        return establishments.findById(code)
                .orElseThrow(() -> new DomainException("El establecimiento " + code + " no existe"));
    }

    private MovementEntity toEntity(Movement m) {
        return new MovementEntity(m.id(), m.date(), m.type(), m.establishmentId(), m.categoryId(),
                m.quantity(), m.destEstablishmentId(), m.destCategoryId(), m.cueros(), m.note());
    }

    private Movement toDomain(MovementEntity me) {
        return new Movement(me.getBusinessId(), me.getDate(), me.getType(), me.getEstablishmentCode(),
                me.getCategoryCode(), me.getQuantity(), me.getDestEstablishmentCode(), me.getDestCategoryCode(),
                me.getCueros(), me.getNote());
    }
}
