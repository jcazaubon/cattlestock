package com.kinetix.cattlestock.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.kinetix.cattlestock.app.LedgerService;
import com.kinetix.cattlestock.app.UserService;
import com.kinetix.cattlestock.data.CategoryEntity;
import com.kinetix.cattlestock.data.UserEntity;
import com.kinetix.cattlestock.domain.Movement;
import com.kinetix.cattlestock.domain.Species;

/**
 * Un stock inicial vacio no es un cero: una categoria sin stock inicial en un establecimiento no forma parte de el
 * (no aparece en su planilla, su existencia ni sus cargas); con un 0 forma parte, sin animales.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StockVacioNoEsCeroTest {

    static final YearMonth SEP = YearMonth.of(2026, 9);
    static final LocalDate FIN = LocalDate.of(2026, 9, 30);

    @Autowired MockMvc mockMvc;
    @Autowired LedgerService service;
    @Autowired UserService userService;

    Long empresa;

    /** N1 tiene toros (10) y vacas (0 explicito); no tiene novillos (vacio). N2 no tiene stock inicial cargado. */
    void configurar() {
        empresa = service.createCompany("Ganadera Norte", SEP);
        service.createEstablishment(empresa, "N1", "Campo Norte");
        service.createEstablishment(empresa, "N2", "Campo Sur");
        service.createCategory(empresa, "toros", "Toros", Species.VACUNOS);
        service.createCategory(empresa, "vacas", "Vacas", Species.VACUNOS);
        service.createCategory(empresa, "novillos", "Novillos", Species.VACUNOS);
        service.setOpening("N1", "toros", 10);
        service.setOpening("N1", "vacas", 0);
        userService.crear("adminnorte", "x", UserEntity.Role.ADMIN, empresa, List.of());
    }

    org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor admin() {
        return user("adminnorte").roles("ADMIN");
    }

    List<String> codigos(List<CategoryEntity> l) {
        return l.stream().map(CategoryEntity::getCode).toList();
    }

    @Test
    void laPlanillaSoloTraeLasCategoriasQueTienenStockInicialIncluidoElCero() {
        configurar();
        var filas = service.planilla("N1", SEP).rows().stream().map(r -> r.category().id()).toList();
        assertEquals(List.of("toros", "vacas"), filas, "novillos esta vacio: no forma parte de N1");
    }

    @Test
    void unEstablecimientoSinStockInicialCargadoUsaTodasLasCategoriasDeLaEmpresa() {
        configurar();
        var filas = service.planilla("N2", SEP).rows().stream().map(r -> r.category().id()).toList();
        assertEquals(List.of("toros", "vacas", "novillos"), filas);
    }

    @Test
    void lasCargasSoloOfrecenLasQueFormanParteDelEstablecimiento() throws Exception {
        configurar();
        assertEquals(List.of("toros", "vacas"), codigos(service.categoriasHabilitadasDe("N1")));
        assertEquals(List.of("toros", "vacas", "novillos"), codigos(service.categoriasHabilitadasDe("N2")));
        mockMvc.perform(get("/grilla/filas").param("establecimiento", "N1").with(admin()))
                .andExpect(content().string(containsString("name=\"compra_vacas\"")))
                .andExpect(content().string(not(containsString("name=\"compra_novillos\""))));
        mockMvc.perform(get("/alta/lineas").param("establecimiento", "N1").with(admin()))
                .andExpect(content().string(containsString("value=\"vacas\"")))
                .andExpect(content().string(not(containsString("value=\"novillos\""))));
    }

    @Test
    void laExistenciaPorEstablecimientoYLaGlobalRespetanLaRegla() {
        configurar();
        var n1 = service.existencia(empresa, FIN, "N1", null, null).stream().map(r -> r.category().id()).toList();
        assertEquals(List.of("toros", "vacas"), n1);
        // N2 usa todas, asi que en el resumen global aparecen todas
        var global = service.existencia(empresa, FIN, null, null, null).stream().map(r -> r.category().id()).toList();
        assertTrue(global.contains("novillos"));
        // el detalle tambien
        var det = service.existenciaDetallada(empresa, FIN, "N1", null, null).stream().map(d -> d.category().id()).toList();
        assertEquals(List.of("toros", "vacas"), det);
    }

    @Test
    void unaCategoriaVaciaQueTuvoMovimientosSigueFormandoParteDelEstablecimiento() {
        configurar();
        // novillos nace en N1: el movimiento la incluye aunque no tenga stock inicial
        service.addMovement(Movement.nacimiento("n1", LocalDate.of(2026, 9, 5), "N1", "novillos", 3));
        var filas = service.planilla("N1", SEP).rows().stream().map(r -> r.category().id()).toList();
        assertTrue(filas.contains("novillos"), "ya tiene movimientos: no se esconden animales");
        assertEquals(3, service.existencia(empresa, FIN, "N1", null, "novillos").get(0).quantity());
    }

    @Test
    void cargarUnCeroIncluyeLaCategoriaYLaPantallaDeStockMuestraTodasParaPoderlo() throws Exception {
        configurar();
        // la pantalla de stock inicial lista todas las categorias de la empresa, tambien las que estan fuera
        mockMvc.perform(get("/configuracion").param("establecimiento", "N1").param("seccion", "establecimientos").with(admin()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"stock_novillos\"")));
        // un campo vacio no crea nada; un 0 la incluye
        mockMvc.perform(post("/configuracion/stock").with(admin()).with(csrf())
                        .param("establecimiento", "N1").param("stock_novillos", ""))
                .andExpect(status().is3xxRedirection());
        assertEquals(List.of("toros", "vacas"), codigos(service.categoriasHabilitadasDe("N1")));
        mockMvc.perform(post("/configuracion/stock").with(admin()).with(csrf())
                        .param("establecimiento", "N1").param("stock_novillos", "0"))
                .andExpect(status().is3xxRedirection());
        assertEquals(List.of("toros", "vacas", "novillos"), codigos(service.categoriasHabilitadasDe("N1")));
    }
}
