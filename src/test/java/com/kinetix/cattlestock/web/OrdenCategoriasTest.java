package com.kinetix.cattlestock.web;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.kinetix.cattlestock.domain.DomainException;
import com.kinetix.cattlestock.domain.Species;

/** El administrador reordena las categorias de la empresa: subir o bajar, y el orden vale en todas partes. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OrdenCategoriasTest {

    static final YearMonth SEP = YearMonth.of(2026, 9);

    @Autowired MockMvc mockMvc;
    @Autowired LedgerService service;
    @Autowired UserService userService;

    Long norte;
    Long sur;

    /** Norte: toros, vacas, novillos, terneros (en ese orden). Sur: sureno. */
    void configurar() {
        norte = service.createCompany("Ganadera Norte", SEP);
        service.createEstablishment(norte, "N1", "Campo Norte");
        for (String[] c : new String[][] {{"toros", "Toros"}, {"vacas", "Vacas"}, {"novillos", "Novillos"}, {"terneros", "Terneros"}}) {
            service.createCategory(norte, c[0], c[1], Species.VACUNOS);
        }
        sur = service.createCompany("Ganadera Sur", SEP);
        service.createEstablishment(sur, "S1", "Campo Sur");
        service.createCategory(sur, "sureno", "Sureno", Species.VACUNOS);
        userService.crear("adminnorte", "x", UserEntity.Role.ADMIN, norte, List.of());
        userService.crear("pepe", "x", UserEntity.Role.CARGA, norte, List.of("N1"));
    }

    org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor admin() {
        return user("adminnorte").roles("ADMIN");
    }

    List<String> orden() {
        return service.categoriasDeEmpresa(norte).stream().map(CategoryEntity::getName).toList();
    }

    @Test
    void subirYBajarIntercambianLugarConLaVecina() {
        configurar();
        service.moveCategory(norte, "novillos", -1); // sube
        assertEquals(List.of("Toros", "Novillos", "Vacas", "Terneros"), orden());
        service.moveCategory(norte, "toros", 1);     // baja
        assertEquals(List.of("Novillos", "Toros", "Vacas", "Terneros"), orden());
        service.moveCategory(norte, "terneros", -1);
        service.moveCategory(norte, "terneros", -1);
        service.moveCategory(norte, "terneros", -1);
        assertEquals(List.of("Terneros", "Novillos", "Toros", "Vacas"), orden());
    }

    @Test
    void enLosExtremosNoHaceNadaYUnaCategoriaAjenaNoExiste() {
        configurar();
        service.moveCategory(norte, "toros", -1);
        service.moveCategory(norte, "terneros", 1);
        assertEquals(List.of("Toros", "Vacas", "Novillos", "Terneros"), orden());
        var e = assertThrows(DomainException.class, () -> service.moveCategory(norte, "sureno", -1));
        assertEquals("La categoria sureno no existe", e.getMessage());
        assertEquals(List.of("Sureno"), service.categoriasDeEmpresa(sur).stream().map(CategoryEntity::getName).toList());
    }

    @Test
    void laPosicionesRepetidasSeNormalizanYElMovimientoSiempreSeNota() {
        configurar();
        // despues de una fusion o una importacion pueden quedar dos categorias con la misma posicion
        service.categoriasDeEmpresa(norte).forEach(c -> {
            if (!c.getCode().equals("toros")) c.setPosition(7);
        });
        service.moveCategory(norte, "terneros", -1);
        List<Integer> posiciones = service.categoriasDeEmpresa(norte).stream().map(CategoryEntity::getPosition).toList();
        assertEquals(List.of(0, 1, 2, 3), posiciones, "quedan numeradas sin repetir");
        assertEquals(List.of("Toros", "Vacas", "Terneros", "Novillos"), orden());
    }

    @Test
    void elOrdenSeVeEnLaPlanillaEnLaExistenciaEnLasCargasYEnElExcelDeStock() throws Exception {
        configurar();
        service.moveCategory(norte, "terneros", -1);
        service.moveCategory(norte, "terneros", -1);
        service.moveCategory(norte, "terneros", -1); // Terneros, Toros, Vacas, Novillos
        var planilla = service.planilla("N1", SEP).rows().stream().map(r -> r.category().name()).toList();
        assertEquals(List.of("Terneros", "Toros", "Vacas", "Novillos"), planilla);
        var existencia = service.existencia(norte, SEP.atEndOfMonth(), null, null, null).stream()
                .map(r -> r.category().name()).toList();
        assertEquals(List.of("Terneros", "Toros", "Vacas", "Novillos"), existencia);
        assertEquals(List.of("Terneros", "Toros", "Vacas", "Novillos"),
                service.categoriasHabilitadasDe("N1").stream().map(CategoryEntity::getName).toList());
        mockMvc.perform(get("/grilla/filas").param("establecimiento", "N1").with(admin()))
                .andExpect(content().string(org.hamcrest.Matchers.matchesPattern(
                        "(?s).*compra_terneros.*compra_toros.*compra_vacas.*compra_novillos.*")));
    }

    @Test
    void laListaTraeBotonesDeSubirYBajarConLosExtremosDeshabilitados() throws Exception {
        configurar();
        mockMvc.perform(get("/configuracion").param("seccion", "categorias").with(admin()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<th>Orden</th>")))
                .andExpect(content().string(containsString("action=\"/configuracion/categorias/vacas/subir\"")))
                .andExpect(content().string(containsString("action=\"/configuracion/categorias/vacas/bajar\"")))
                .andExpect(content().string(containsString("aria-label=\"Subir Vacas\"")))
                // la primera no puede subir y la ultima no puede bajar
                .andExpect(content().string(org.hamcrest.Matchers.matchesPattern(
                        "(?s).*title=\"Subir\" aria-label=\"Subir Toros\"\\s+disabled.*")))
                .andExpect(content().string(org.hamcrest.Matchers.matchesPattern(
                        "(?s).*title=\"Bajar\" aria-label=\"Bajar Terneros\"\\s+disabled.*")));
    }

    @Test
    void lasAccionesDePantallaMuevenYSoloLaEmpresaDelAdministrador() throws Exception {
        configurar();
        mockMvc.perform(post("/configuracion/categorias/novillos/subir").with(admin()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/configuracion?seccion=categorias"));
        assertEquals(List.of("Toros", "Novillos", "Vacas", "Terneros"), orden());
        mockMvc.perform(post("/configuracion/categorias/novillos/bajar").with(admin()).with(csrf()))
                .andExpect(redirectedUrl("/configuracion?seccion=categorias"));
        assertEquals(List.of("Toros", "Vacas", "Novillos", "Terneros"), orden());
        // una categoria de otra empresa vuelve con el aviso y no mueve nada
        mockMvc.perform(post("/configuracion/categorias/sureno/subir").with(admin()).with(csrf()))
                .andExpect(redirectedUrl("/configuracion?seccion=categorias&error=La+categoria+sureno+no+existe"));
        // quien solo carga no tiene acceso
        mockMvc.perform(post("/configuracion/categorias/toros/bajar").with(user("pepe").roles("CARGA")).with(csrf()))
                .andExpect(status().isForbidden());
        assertEquals(List.of("Toros", "Vacas", "Novillos", "Terneros"), orden());
    }
}
