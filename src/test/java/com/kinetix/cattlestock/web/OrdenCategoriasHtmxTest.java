package com.kinetix.cattlestock.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.kinetix.cattlestock.domain.Species;

/** Mover una categoria con htmx: vuelve solo la tabla, sin recargar la pagina, y sin htmx todo sigue andando. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OrdenCategoriasHtmxTest {

    static final YearMonth SEP = YearMonth.of(2026, 9);

    @Autowired MockMvc mockMvc;
    @Autowired LedgerService service;
    @Autowired UserService userService;

    Long norte;
    Long sur;

    void configurar() {
        norte = service.createCompany("Ganadera Norte", SEP);
        service.createEstablishment(norte, "N1", "Campo Norte");
        for (String[] c : new String[][] {{"toros", "Toros"}, {"vacas", "Vacas"}, {"novillos", "Novillos"}}) {
            service.createCategory(norte, c[0], c[1], Species.VACUNOS);
        }
        sur = service.createCompany("Ganadera Sur", SEP);
        service.createEstablishment(sur, "S1", "Campo Sur");
        service.createCategory(sur, "sureno", "Sureno", Species.VACUNOS);
        userService.crear("adminnorte", "x", UserEntity.Role.ADMIN, norte, List.of());
    }

    org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor admin() {
        return user("adminnorte").roles("ADMIN");
    }

    List<String> orden() {
        return service.categoriasDeEmpresa(norte).stream().map(CategoryEntity::getName).toList();
    }

    @Test
    void conHtmxDevuelveSoloLaTablaYaReordenadaYMarcaLaFilaMovida() throws Exception {
        configurar();
        mockMvc.perform(post("/configuracion/categorias/novillos/subir").with(admin()).with(csrf())
                        .header("HX-Request", "true"))
                .andExpect(status().isOk())
                // es un fragmento, no una pagina: no trae barra ni <html>
                .andExpect(content().string(not(containsString("<!DOCTYPE"))))
                .andExpect(content().string(not(containsString("<header"))))
                .andExpect(content().string(containsString("id=\"tabla-categorias\"")))
                // el orden ya cambio
                .andExpect(content().string(matchesPattern("(?s).*Toros.*Novillos.*Vacas.*")))
                // y la fila que se movio queda marcada para resaltarla
                .andExpect(content().string(matchesPattern("(?s).*<tr class=\" movida\">\\s*<td class=\"orden\">.*Novillos.*")))
                .andExpect(content().string(not(matchesPattern("(?s).*<tr class=\" movida\">.*<tr class=\" movida\">.*"))));
        assertEquals(List.of("Toros", "Novillos", "Vacas"), orden());
    }

    @Test
    void lasFlechasLlevanIdYLosAtributosDeHtmxParaConservarElFocoYNoRecargar() throws Exception {
        configurar();
        mockMvc.perform(post("/configuracion/categorias/vacas/bajar").with(admin()).with(csrf())
                        .header("HX-Request", "true"))
                .andExpect(content().string(containsString("id=\"subir-vacas\"")))
                .andExpect(content().string(containsString("id=\"bajar-vacas\"")))
                .andExpect(content().string(containsString("hx-post=\"/configuracion/categorias/vacas/subir\"")))
                .andExpect(content().string(containsString("hx-target=\"#tabla-categorias\"")))
                .andExpect(content().string(containsString("hx-swap=\"outerHTML\"")));
    }

    @Test
    void alLlegarAlExtremoLaFlechaQuedaDeshabilitadaEnLaTablaDevuelta() throws Exception {
        configurar();
        mockMvc.perform(post("/configuracion/categorias/vacas/subir").with(admin()).with(csrf())
                        .header("HX-Request", "true"))
                .andExpect(content().string(matchesPattern("(?s).*id=\"subir-vacas\"[^>]*\\s+disabled.*")));
        assertEquals(List.of("Vacas", "Toros", "Novillos"), orden());
    }

    @Test
    void unErrorConHtmxVuelveEnLaMismaTablaYSinRedirigir() throws Exception {
        configurar();
        mockMvc.perform(post("/configuracion/categorias/sureno/subir").with(admin()).with(csrf())
                        .header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("La categoria sureno no existe")))
                .andExpect(content().string(containsString("id=\"tabla-categorias\"")))
                // y la fila no se marca como movida
                .andExpect(content().string(not(containsString("movida"))));
        assertEquals(List.of("Toros", "Vacas", "Novillos"), orden());
    }

    @Test
    void sinHtmxSigueFuncionandoComoUnFormularioComun() throws Exception {
        configurar();
        mockMvc.perform(post("/configuracion/categorias/novillos/subir").with(admin()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/configuracion?seccion=categorias"));
        assertEquals(List.of("Toros", "Novillos", "Vacas"), orden());
        mockMvc.perform(post("/configuracion/categorias/sureno/subir").with(admin()).with(csrf()))
                .andExpect(redirectedUrl("/configuracion?seccion=categorias&error=La+categoria+sureno+no+existe"));
    }

    @Test
    void laPaginaCompletaTraeLaTablaEnSuContenedorYLasFlechasConHtmx() throws Exception {
        configurar();
        mockMvc.perform(get("/configuracion").param("seccion", "categorias").with(admin()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<!DOCTYPE html>")))
                .andExpect(content().string(containsString("id=\"tabla-categorias\"")))
                .andExpect(content().string(containsString("hx-post=\"/configuracion/categorias/toros/bajar\"")))
                // la accion de la tabla sigue estando para quien no tenga JavaScript
                .andExpect(content().string(containsString("action=\"/configuracion/categorias/toros/bajar\"")))
                // y la tabla se ve una sola vez, no dos
                .andExpect(content().string(not(matchesPattern("(?s).*id=\"tabla-categorias\".*id=\"tabla-categorias\".*"))));
    }

    @Test
    void laEdicionDelNombreDeUnaFilaSigueFuncionandoDentroDelFragmento() throws Exception {
        configurar();
        mockMvc.perform(get("/configuracion").param("seccion", "categorias").param("editar", "vacas").with(admin()))
                .andExpect(content().string(containsString("action=\"/configuracion/categorias/vacas/nombre\"")))
                .andExpect(content().string(containsString("form=\"renombrar\"")))
                .andExpect(content().string(containsString("aria-label=\"Guardar nombre\"")));
    }
}
