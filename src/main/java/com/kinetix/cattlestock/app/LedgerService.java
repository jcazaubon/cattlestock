package com.kinetix.cattlestock.app;

import java.text.Normalizer;
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
 * Cada empresa tiene su propio Ledger: el armado carga solo los establecimientos, categorias, stocks
 * iniciales y cierres de la empresa, y los movimientos del componente conectado del establecimiento
 * pedido (se expande por traslados hasta que las referencias y los saldos cierran). Dos empresas nunca
 * se mezclan: un traslado hacia un establecimiento de otra empresa falla porque ese establecimiento
 * no existe en el Ledger.
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

    /** Da de alta una empresa y devuelve su id. */
    @Transactional
    public Long createCompany(String name, YearMonth startMonth) {
        if (name == null || name.isBlank()) {
            throw new DomainException("La empresa necesita nombre");
        }
        boolean repetida = companies.findAll().stream()
                .anyMatch(c -> c.getName().equalsIgnoreCase(name.trim()));
        if (repetida) {
            throw new DomainException("Ya hay una empresa llamada " + name.trim());
        }
        long id = companies.findAll().stream().mapToLong(CompanyEntity::getId).max().orElse(0L) + 1;
        companies.save(new CompanyEntity(id, name.trim(), startMonth.toString()));
        return id;
    }

    @Transactional
    public void createEstablishment(Long companyId, String code, String name) {
        company(companyId);
        if (establishments.existsById(code)) {
            throw new DomainException("El establecimiento " + code + " ya existe");
        }
        establishments.save(new EstablishmentEntity(companyId, code, name));
    }

    /** Cambia el nombre de un establecimiento. El codigo no cambia: es la clave de todo lo que lo referencia. */
    @Transactional
    public void renameEstablishment(String code, String name) {
        EstablishmentEntity est = requireEstablishment(code);
        if (name == null || name.isBlank()) {
            throw new DomainException("El establecimiento necesita nombre");
        }
        String nuevo = name.trim();
        boolean repetido = establishments.findByCompanyIdOrderByCodeAsc(est.getCompanyId()).stream()
                .anyMatch(e -> !e.getCode().equals(code) && e.getName().equalsIgnoreCase(nuevo));
        if (repetido) {
            throw new DomainException("Ya hay un establecimiento llamado " + nuevo);
        }
        est.setName(nuevo);
        establishments.save(est);
    }

    /** Un codigo corto libre para un establecimiento nuevo, a partir del nombre ("La Marta" da "lamarta"). */
    @Transactional(readOnly = true)
    public String codigoDisponible(String name) {
        String limpio = Normalizer.normalize(name == null ? "" : name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase().replaceAll("[^a-z0-9]", "");
        if (limpio.isEmpty()) limpio = "est";
        if (limpio.length() > 10) limpio = limpio.substring(0, 10);
        String candidato = limpio;
        int n = 2;
        while (establishments.existsById(candidato)) {
            candidato = limpio + n++;
        }
        return candidato;
    }

    /** Una categoria a importar: su nombre y la especie. */
    public record NuevaCategoria(String name, Species species) {
    }

    /** El catalogo de categorias de la empresa, en el orden de la planilla. */
    @Transactional(readOnly = true)
    public List<CategoryEntity> categoriasDeEmpresa(Long companyId) {
        return categories.findByCompanyIdOrderByPositionAscIdAsc(companyId);
    }

    /** Las categorias que puede usar un establecimiento: las de su empresa. */
    @Transactional(readOnly = true)
    public List<CategoryEntity> categoriasDe(String establishmentCode) {
        return categories.findByCompanyIdOrderByPositionAscIdAsc(companyOf(establishmentCode));
    }

    /**
     * Las categorias de un establecimiento que se pueden usar para cargar movimientos: las habilitadas que forman
     * parte del establecimiento (ver {@link #categoriasIncluidas}).
     */
    @Transactional(readOnly = true)
    public List<CategoryEntity> categoriasHabilitadasDe(String establishmentCode) {
        Set<String> incluidas = categoriasIncluidas(List.of(establishmentCode));
        return categoriasDe(establishmentCode).stream().filter(CategoryEntity::isEnabled)
                .filter(c -> incluidas == null || incluidas.contains(c.getCode())).toList();
    }

    /**
     * Los codigos de las categorias que forman parte de alguno de esos establecimientos, o null si forman parte
     * todas. Una categoria forma parte de un establecimiento si su stock inicial esta cargado (un 0 cuenta: el
     * establecimiento la tiene, sin animales) o si ya tuvo movimientos ahi. Una categoria con el stock inicial
     * vacio no se confunde con un cero: no forma parte del establecimiento y no aparece en su planilla ni en sus
     * cargas. Un establecimiento sin ningun stock inicial cargado todavia usa todas las de la empresa.
     */
    private Set<String> categoriasIncluidas(Collection<String> establishmentCodes) {
        Set<String> out = new LinkedHashSet<>();
        for (String codigo : establishmentCodes) {
            List<OpeningEntity> aperturas = openings.findByEstablishmentCodeIn(List.of(codigo));
            if (aperturas.isEmpty()) return null;
            for (OpeningEntity o : aperturas) out.add(o.getCategoryCode());
            for (MovementEntity m : movements.findByEstablishmentCodeInOrDestEstablishmentCodeInOrderByDateAscIdAsc(
                    List.of(codigo), List.of(codigo))) {
                if (m.isDeleted()) continue;
                if (codigo.equals(m.getEstablishmentCode())) out.add(m.getCategoryCode());
                boolean destinoAca = m.getDestEstablishmentCode() == null
                        ? codigo.equals(m.getEstablishmentCode()) : codigo.equals(m.getDestEstablishmentCode());
                if (destinoAca && m.getDestCategoryCode() != null) out.add(m.getDestCategoryCode());
            }
        }
        return out;
    }

    private Set<String> deshabilitadas(Long companyId) {
        Set<String> out = new LinkedHashSet<>();
        for (CategoryEntity c : categories.findByCompanyIdOrderByPositionAscIdAsc(companyId)) {
            if (!c.isEnabled()) out.add(c.getCode());
        }
        return out;
    }

    /** No se cargan movimientos sobre una categoria deshabilitada, venga la peticion de donde venga. */
    private void verificarHabilitadas(Long companyId, List<Movement> movimientos) {
        Set<String> apagadas = deshabilitadas(companyId);
        if (apagadas.isEmpty()) return;
        for (Movement m : movimientos) {
            for (String codigo : new String[] {m.categoryId(), m.destCategoryId()}) {
                if (codigo != null && apagadas.contains(codigo)) {
                    String nombre = categories.findByCompanyIdAndCode(companyId, codigo)
                            .map(CategoryEntity::getName).orElse(codigo);
                    throw new DomainException("La categoria " + nombre
                            + " esta deshabilitada: habilitala en Configuracion para cargar movimientos.");
                }
            }
        }
    }

    private CategoryEntity categoria(Long companyId, String code) {
        return categories.findByCompanyIdAndCode(companyId, code)
                .orElseThrow(() -> new DomainException("La categoria " + code + " no existe"));
    }

    /** Cambia el nombre de una categoria. El codigo no cambia: lo usan el stock y los movimientos. */
    @Transactional
    public void renameCategory(Long companyId, String code, String name) {
        CategoryEntity c = categoria(companyId, code);
        if (name == null || name.isBlank()) throw new DomainException("La categoria necesita nombre");
        String nuevo = name.trim();
        boolean repetida = categories.findByCompanyIdOrderByPositionAscIdAsc(companyId).stream()
                .anyMatch(o -> !o.getCode().equals(code) && o.getName().equalsIgnoreCase(nuevo));
        if (repetida) throw new DomainException("Ya hay una categoria llamada " + nuevo);
        c.setName(nuevo);
        categories.save(c);
    }

    /**
     * Sube (delta -1) o baja (delta +1) una categoria en el orden de la planilla, intercambiando lugar con la vecina.
     * Las posiciones se normalizan antes, porque despues de importar o fusionar categorias pueden repetirse. En el
     * extremo de la lista no hace nada. El orden vale para toda la empresa: planillas, existencia, cargas y Excel.
     */
    @Transactional
    public void moveCategory(Long companyId, String code, int delta) {
        List<CategoryEntity> lista = new java.util.ArrayList<>(categories.findByCompanyIdOrderByPositionAscIdAsc(companyId));
        int idx = -1;
        for (int i = 0; i < lista.size(); i++) {
            lista.get(i).setPosition(i);
            if (lista.get(i).getCode().equals(code)) idx = i;
        }
        if (idx < 0) throw new DomainException("La categoria " + code + " no existe");
        int destino = idx + (delta < 0 ? -1 : 1);
        if (destino >= 0 && destino < lista.size()) {
            lista.get(idx).setPosition(destino);
            lista.get(destino).setPosition(idx);
        }
        categories.saveAll(lista);
    }

    @Transactional
    public void setCategoryEnabled(Long companyId, String code, boolean enabled) {
        CategoryEntity c = categoria(companyId, code);
        c.setEnabled(enabled);
        categories.save(c);
    }

    /**
     * Da de baja una categoria. Solo si nunca se uso: sin stock inicial ni movimientos en ningun establecimiento
     * de la empresa. Si ya tiene historia, borrarla dejaria la planilla sin explicar de donde sale el stock: ahi
     * corresponde deshabilitarla.
     */
    @Transactional
    public void deleteCategory(Long companyId, String code) {
        CategoryEntity c = categoria(companyId, code);
        List<String> propios = establishments.findByCompanyIdOrderByCodeAsc(companyId).stream()
                .map(EstablishmentEntity::getCode).toList();
        List<OpeningEntity> aperturas = propios.isEmpty() ? List.<OpeningEntity>of()
                : openings.findByEstablishmentCodeIn(propios).stream()
                        .filter(o -> o.getCategoryCode().equals(code)).toList();
        boolean usada = !propios.isEmpty()
                && (movements.existsByCategoryCodeAndEstablishmentCodeIn(code, propios)
                    || movements.existsByDestCategoryCodeAndEstablishmentCodeIn(code, propios));
        if (usada || aperturas.stream().anyMatch(o -> o.getQuantity() != 0)) {
            throw new DomainException("No se puede dar de baja \"" + c.getName()
                    + "\": tiene stock o movimientos. Podes deshabilitarla.");
        }
        openings.deleteAll(aperturas);
        categories.delete(c);
    }

    /** Un codigo libre para una categoria nueva de la empresa, a partir del nombre. */
    @Transactional(readOnly = true)
    public String codigoCategoriaDisponible(Long companyId, String name) {
        String limpio = Normalizer.normalize(name == null ? "" : name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase().replaceAll("[^a-z0-9]", "");
        if (limpio.isEmpty()) limpio = "cat";
        if (limpio.length() > 20) limpio = limpio.substring(0, 20);
        String candidato = limpio;
        int n = 2;
        while (categories.existsByCompanyIdAndCode(companyId, candidato)) {
            candidato = limpio + n++;
        }
        return candidato;
    }

    @Transactional
    public void createCategory(Long companyId, String code, String name, Species species) {
        company(companyId);
        if (code == null || code.isBlank() || name == null || name.isBlank() || species == null) {
            throw new DomainException("La categoria necesita codigo, nombre y especie");
        }
        if (categories.existsByCompanyIdAndCode(companyId, code)) {
            throw new DomainException("La categoria " + code + " ya existe");
        }
        if (categories.existsByCompanyIdAndNameIgnoreCase(companyId, name.trim())) {
            throw new DomainException("Ya hay una categoria llamada " + name.trim());
        }
        categories.save(new CategoryEntity(companyId, code, name.trim(), species,
                (int) categories.countByCompanyId(companyId)));
    }

    /**
     * Importa varias categorias a la vez: todas o ninguna. Las que ya existen (mismo nombre) se saltean sin
     * error, para poder importar dos veces el mismo archivo. Devuelve cuantas creo.
     */
    @Transactional
    public int importCategories(Long companyId, List<NuevaCategoria> nuevas) {
        company(companyId);
        int creadas = 0;
        for (NuevaCategoria n : nuevas) {
            if (n.name() == null || n.name().isBlank() || n.species() == null) continue;
            if (categories.existsByCompanyIdAndNameIgnoreCase(companyId, n.name().trim())) continue;
            createCategory(companyId, codigoCategoriaDisponible(companyId, n.name()), n.name(), n.species());
            creadas++;
        }
        return creadas;
    }

    /** Un stock inicial a aplicar: establecimiento, categoria y cantidad. */
    public record NuevaApertura(String establishmentCode, String categoryCode, int quantity) {
    }

    /** El stock inicial cargado de la empresa, por "establecimiento|categoria". */
    @Transactional(readOnly = true)
    public java.util.Map<String, Integer> aperturasDeEmpresa(Long companyId) {
        List<String> codigos = establishments.findByCompanyIdOrderByCodeAsc(companyId).stream()
                .map(EstablishmentEntity::getCode).toList();
        java.util.Map<String, Integer> out = new java.util.HashMap<>();
        if (codigos.isEmpty()) return out;
        for (OpeningEntity o : openings.findByEstablishmentCodeIn(codigos)) {
            out.put(o.getEstablishmentCode() + "|" + o.getCategoryCode(), o.getQuantity());
        }
        return out;
    }

    /**
     * Aplica el stock inicial de varios establecimientos y categorias de la empresa de una vez. Se validan todos
     * contra un solo Ledger (stock no negativo, mes inicial abierto, sin dejar saldos negativos con los
     * movimientos que ya haya) y solo si todos pasan se guarda: todo o nada. Devuelve cuantos valores cambiaron.
     */
    @Transactional
    public int importOpenings(Long companyId, List<NuevaApertura> aperturas) {
        company(companyId);
        List<String> propios = establishments.findByCompanyIdOrderByCodeAsc(companyId).stream()
                .map(EstablishmentEntity::getCode).toList();
        Ledger ledger = ledgerFor(companyId, propios);
        for (NuevaApertura a : aperturas) {
            if (!propios.contains(a.establishmentCode())) {
                throw new DomainException("El establecimiento " + a.establishmentCode() + " no es de la empresa");
            }
            ledger.setOpening(a.establishmentCode(), a.categoryCode(), a.quantity());
        }
        int cambiados = 0;
        for (NuevaApertura a : aperturas) {
            OpeningEntity o = openings.findByEstablishmentCodeAndCategoryCode(a.establishmentCode(), a.categoryCode())
                    .orElseGet(() -> new OpeningEntity(a.establishmentCode(), a.categoryCode()));
            if (o.getId() == null || o.getQuantity() != a.quantity()) cambiados++;
            o.setQuantity(a.quantity());
            openings.save(o);
        }
        return cambiados;
    }

    @Transactional
    public void setOpening(String establishmentCode, String categoryCode, int quantity) {
        Ledger ledger = ledgerFor(companyOf(establishmentCode), List.of(establishmentCode));
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
        Long empresa = companyOf(m.establishmentId());
        verificarHabilitadas(empresa, List.of(m));
        ledgerFor(empresa, codes).add(m);
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
        Long empresa = companyOf(batch.get(0).establishmentId());
        verificarHabilitadas(empresa, batch);
        ledgerFor(empresa, codes).addAll(batch);
        for (Movement m : batch) movements.save(toEntity(m));
    }

    // --- Historial: ver, modificar y borrar (logico) movimientos ---

    /** Lo que se puede corregir de un movimiento ya cargado. */
    public record CambiosMovimiento(LocalDate fecha, String categoria, int cantidad, String destinoCategoria,
                                    Integer cueros, String nota) {
    }

    /** Los movimientos de esos establecimientos en un mes, incluidos los borrados, del mas nuevo al mas viejo. */
    @Transactional(readOnly = true)
    public List<MovementEntity> listarMovimientos(Collection<String> establishmentCodes, YearMonth mes) {
        if (establishmentCodes.isEmpty()) return List.of();
        return movements.findByEstablishmentCodeInAndDateBetweenOrderByDateDescIdDesc(
                establishmentCodes, mes.atDay(1), mes.atEndOfMonth());
    }

    private MovementEntity movimientoDe(Long companyId, Long id) {
        MovementEntity me = movements.findById(id).orElseThrow(() -> new DomainException("El movimiento no existe"));
        if (!companyId.equals(companyOf(me.getEstablishmentCode()))) {
            throw new DomainException("El movimiento no existe");
        }
        return me;
    }

    /** El establecimiento donde se cargo un movimiento (falla si no es de la empresa). */
    @Transactional(readOnly = true)
    public String establecimientoDeMovimiento(Long companyId, Long id) {
        return movimientoDe(companyId, id).getEstablishmentCode();
    }

    private static Set<String> establecimientosDe(MovementEntity me) {
        Set<String> codes = new LinkedHashSet<>();
        codes.add(me.getEstablishmentCode());
        if (me.getDestEstablishmentCode() != null) codes.add(me.getDestEstablishmentCode());
        return codes;
    }

    /** Un movimiento de un mes cerrado no se toca: hay que reabrir la planilla antes. */
    private void verificarMesAbierto(Ledger ledger, MovementEntity me, LocalDate fecha) {
        for (String est : establecimientosDe(me)) {
            if (ledger.isClosed(est, fecha)) {
                throw new DomainException("La planilla de " + ledger.establishment(est).name() + " de "
                        + YearMonth.from(fecha) + " esta cerrada: reabrila para modificar sus movimientos.");
            }
        }
    }

    /**
     * Corrige un movimiento: la fecha, la categoria (y la de destino en cambios y traslados), la cantidad, los
     * cueros y la nota. El tipo, el establecimiento y el establecimiento de destino no cambian: para eso se
     * elimina y se carga de nuevo. Se aplica el cambio y se reconstruye el libro de la empresa: si algo de lo que
     * sigue en el historial deja de cerrar (por ejemplo, un saldo negativo mas adelante), se revierte y se avisa.
     */
    @Transactional
    public void actualizarMovimiento(Long companyId, Long id, String usuario, CambiosMovimiento c) {
        MovementEntity me = movimientoDe(companyId, id);
        if (me.isDeleted()) throw new DomainException("El movimiento esta eliminado: restauralo para modificarlo");
        boolean conDestino = me.getDestCategoryCode() != null;
        boolean mortandad = me.getType() == com.kinetix.cattlestock.domain.MovementType.MORTANDAD;
        // armar el movimiento nuevo valida lo basico (cantidad, cambio a una categoria distinta, cueros <= muertes...)
        Movement nuevo = new Movement(me.getBusinessId(), c.fecha(), me.getType(), me.getEstablishmentCode(),
                c.categoria(), c.cantidad(), me.getDestEstablishmentCode(),
                conDestino ? c.destinoCategoria() : null, mortandad ? c.cueros() : null,
                c.nota() == null || c.nota().isBlank() ? null : c.nota().trim());
        Long empresa = companyId;
        Set<String> codes = establecimientosDe(me);
        Ledger actual = ledgerFor(empresa, codes);
        verificarMesAbierto(actual, me, me.getDate());
        verificarMesAbierto(actual, me, nuevo.date());
        if (!nuevo.categoryId().equals(me.getCategoryCode())
                || (conDestino && !nuevo.destCategoryId().equals(me.getDestCategoryCode()))) {
            verificarHabilitadas(empresa, List.of(nuevo));
        }
        LocalDate fechaAntes = me.getDate();
        String categoriaAntes = me.getCategoryCode();
        int cantidadAntes = me.getQuantity();
        String destinoAntes = me.getDestCategoryCode();
        Integer cuerosAntes = me.getCueros();
        String notaAntes = me.getNote();
        String modificadoPorAntes = me.getModifiedBy();
        java.time.LocalDateTime modificadoEnAntes = me.getModifiedAt();
        me.applyChanges(nuevo.date(), nuevo.categoryId(), nuevo.quantity(), nuevo.destCategoryId(), nuevo.cueros(),
                nuevo.note(), usuario, java.time.LocalDateTime.now());
        try {
            movements.saveAndFlush(me);
            ledgerFor(empresa, codes);
        } catch (DomainException e) {
            me.applyChanges(fechaAntes, categoriaAntes, cantidadAntes, destinoAntes, cuerosAntes, notaAntes,
                    modificadoPorAntes, modificadoEnAntes);
            throw new DomainException("No se puede guardar el cambio: " + e.getMessage());
        }
    }

    /** Borra un movimiento en forma logica: queda en el historial, se puede restaurar y no cuenta en el stock. */
    @Transactional
    public void eliminarMovimiento(Long companyId, Long id, String usuario) {
        MovementEntity me = movimientoDe(companyId, id);
        if (me.isDeleted()) return;
        Set<String> codes = establecimientosDe(me);
        verificarMesAbierto(ledgerFor(companyId, codes), me, me.getDate());
        me.markDeleted(usuario, java.time.LocalDateTime.now());
        try {
            movements.saveAndFlush(me);
            ledgerFor(companyId, codes);
        } catch (DomainException e) {
            me.restore();
            throw new DomainException("No se puede eliminar el movimiento: " + e.getMessage());
        }
    }

    /** Vuelve a contar un movimiento eliminado, si el historial lo permite. */
    @Transactional
    public void restaurarMovimiento(Long companyId, Long id) {
        MovementEntity me = movimientoDe(companyId, id);
        if (!me.isDeleted()) return;
        Set<String> codes = establecimientosDe(me);
        verificarMesAbierto(ledgerFor(companyId, codes), me, me.getDate());
        String por = me.getDeletedBy();
        java.time.LocalDateTime en = me.getDeletedAt();
        me.restore();
        try {
            movements.saveAndFlush(me);
            ledgerFor(companyId, codes);
        } catch (DomainException e) {
            me.markDeleted(por, en);
            throw new DomainException("No se puede restaurar el movimiento: " + e.getMessage());
        }
    }

    // --- Consultas ---

    @Transactional(readOnly = true)
    public Planilla planilla(String establishmentCode, YearMonth month) {
        Long companyId = companyOf(establishmentCode);
        Planilla p = ledgerFor(companyId, List.of(establishmentCode)).planilla(establishmentCode, month);
        Set<String> incluidas = categoriasIncluidas(List.of(establishmentCode));
        if (incluidas != null) {
            p = new Planilla(p.establishment(), p.month(), p.closed(),
                    p.rows().stream().filter(r -> incluidas.contains(r.category().id())).toList());
        }
        Set<String> apagadas = deshabilitadas(companyId);
        if (apagadas.isEmpty()) return p;
        // una categoria deshabilitada que no tiene nada que mostrar sale de la planilla; si tiene stock o movimientos, se queda
        return new Planilla(p.establishment(), p.month(), p.closed(), p.rows().stream()
                .filter(r -> !(apagadas.contains(r.category().id()) && r.anterior() == 0 && r.finalStock() == 0
                        && r.columns().values().stream().allMatch(v -> v == 0)))
                .toList());
    }

    /** Vista general de existencia de una empresa a una fecha. Los filtros en null no filtran. */
    @Transactional(readOnly = true)
    public List<Ledger.ExistenceRow> existencia(Long companyId, LocalDate asOf, String establishmentCode,
                                                Species species, String categoryCode) {
        Set<String> apagadas = deshabilitadas(companyId);
        List<String> alcance = alcance(companyId, establishmentCode);
        Set<String> incluidas = categoriasIncluidas(alcance);
        return ledgerFor(companyId, alcance)
                .existencia(asOf, vacioANull(establishmentCode), species, vacioANull(categoryCode)).stream()
                .filter(r -> incluidas == null || incluidas.contains(r.category().id()))
                .filter(r -> !(apagadas.contains(r.category().id()) && r.quantity() == 0))
                .toList();
    }

    /** Existencia a una fecha con inicio de mes y movimientos del mes hasta esa fecha. */
    @Transactional(readOnly = true)
    public List<Ledger.ExistenceDetail> existenciaDetallada(Long companyId, LocalDate asOf,
                                                            String establishmentCode, Species species,
                                                            String categoryCode) {
        Set<String> apagadas = deshabilitadas(companyId);
        List<String> alcance = alcance(companyId, establishmentCode);
        Set<String> incluidas = categoriasIncluidas(alcance);
        return ledgerFor(companyId, alcance)
                .existenciaDetallada(asOf, vacioANull(establishmentCode), species, vacioANull(categoryCode)).stream()
                .filter(d -> incluidas == null || incluidas.contains(d.category().id()))
                .filter(d -> !(apagadas.contains(d.category().id()) && d.quantity() == 0
                        && d.entradas() == 0 && d.salidas() == 0))
                .toList();
    }

    private List<String> alcance(Long companyId, String establishmentCode) {
        if (establishmentCode != null && !establishmentCode.isBlank()) {
            if (!companyId.equals(companyOf(establishmentCode))) {
                throw new DomainException("El establecimiento " + establishmentCode + " no existe");
            }
            return List.of(establishmentCode);
        }
        return establishments.findByCompanyIdOrderByCodeAsc(companyId).stream()
                .map(EstablishmentEntity::getCode).toList();
    }

    private static String vacioANull(String valor) {
        return valor == null || valor.isBlank() ? null : valor;
    }

    // --- Cierre de mes ---

    @Transactional
    public void closeMonth(String establishmentCode, YearMonth month) {
        ledgerFor(companyOf(establishmentCode), List.of(establishmentCode)).closeMonth(establishmentCode, month);
        MonthCloseEntity mc = closes.findById(establishmentCode)
                .orElseGet(() -> new MonthCloseEntity(establishmentCode, month.toString()));
        mc.setClosedThrough(month.toString());
        closes.save(mc);
    }

    @Transactional
    public void reopenLastMonth(String establishmentCode) {
        Long companyId = companyOf(establishmentCode);
        CompanyEntity company = company(companyId);
        ledgerFor(companyId, List.of(establishmentCode)).reopenLastMonth(establishmentCode);
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

    private Ledger ledgerFor(Long companyId, Collection<String> requested) {
        CompanyEntity company = company(companyId);
        YearMonth startMonth = YearMonth.parse(company.getStartMonth());

        Ledger ledger = new Ledger(company.getName(), startMonth);

        List<EstablishmentEntity> propios = establishments.findByCompanyIdOrderByCodeAsc(companyId);
        Set<String> codigos = new LinkedHashSet<>();
        for (EstablishmentEntity e : propios) {
            ledger.addEstablishment(e.getCode(), e.getName());
            codigos.add(e.getCode());
        }
        // el catalogo es de la empresa: cada establecimiento tiene todas las categorias
        List<CategoryEntity> catalogo = categories.findByCompanyIdOrderByPositionAscIdAsc(companyId);
        for (EstablishmentEntity e : propios) {
            for (CategoryEntity c : catalogo) {
                ledger.addCategory(e.getCode(), c.getCode(), c.getName(), c.getSpecies());
            }
        }
        if (!codigos.isEmpty()) {
            for (OpeningEntity o : openings.findByEstablishmentCodeIn(codigos)) {
                ledger.setOpening(o.getEstablishmentCode(), o.getCategoryCode(), o.getQuantity());
            }
        }

        // Movimientos del componente conectado: se expande por los traslados hasta cerrar.
        Set<String> scope = new LinkedHashSet<>();
        for (String r : requested) {
            if (codigos.contains(r)) scope.add(r);
        }
        List<MovementEntity> loaded = List.of();
        while (!scope.isEmpty()) {
            // los movimientos borrados (logicamente) no cuentan: ni para el stock ni para conectar establecimientos
            loaded = movements.findByEstablishmentCodeInOrDestEstablishmentCodeInOrderByDateAscIdAsc(scope, scope)
                    .stream().filter(me -> !me.isDeleted()).toList();
            Set<String> next = new LinkedHashSet<>(scope);
            for (MovementEntity me : loaded) {
                if (me.getEstablishmentCode() != null && codigos.contains(me.getEstablishmentCode())) {
                    next.add(me.getEstablishmentCode());
                }
                if (me.getDestEstablishmentCode() != null && codigos.contains(me.getDestEstablishmentCode())) {
                    next.add(me.getDestEstablishmentCode());
                }
            }
            if (next.size() == scope.size()) break;
            scope = next;
        }
        for (MovementEntity me : loaded) {
            ledger.add(toDomain(me));
        }

        // Los cierres se rehacen en orden, a partir del estado persistido.
        for (MonthCloseEntity mc : closes.findAll()) {
            if (!codigos.contains(mc.getEstablishmentCode())) continue;
            YearMonth target = YearMonth.parse(mc.getClosedThrough());
            YearMonth next = startMonth;
            while (!next.isAfter(target)) {
                ledger.closeMonth(mc.getEstablishmentCode(), next);
                next = next.plusMonths(1);
            }
        }
        return ledger;
    }

    private CompanyEntity company(Long id) {
        if (id == null) throw new DomainException("La empresa no esta configurada");
        return companies.findById(id)
                .orElseThrow(() -> new DomainException("La empresa no esta configurada"));
    }

    /** La empresa a la que pertenece un establecimiento. */
    @Transactional(readOnly = true)
    public Long companyOf(String establishmentCode) {
        Long id = requireEstablishment(establishmentCode).getCompanyId();
        if (id == null) throw new DomainException("El establecimiento " + establishmentCode + " no tiene empresa");
        return id;
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
