package com.kinetix.cattlestock.web;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.Year;
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
import com.kinetix.cattlestock.data.UserEntity;
import com.kinetix.cattlestock.domain.Movement;
import com.kinetix.cattlestock.domain.Species;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;

/** El periodo (mes y año) de la planilla y su exportacion a PDF. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PlanillaPeriodoYPdfTest {

    @Autowired MockMvc mockMvc;
    @Autowired LedgerService service;
    @Autowired UserService userService;

    void configurar() {
        Long empresa = service.createCompany("Ganadera Norte", YearMonth.of(2026, 9));
        service.createEstablishment(empresa, "N1", "Campo Norte");
        service.createCategory(empresa, "toros", "Toros", Species.VACUNOS);
        service.setOpening("N1", "toros", 10);
        service.addMovement(Movement.compra("c1", LocalDate.of(2026, 9, 3), "N1", "toros", 2));
        userService.crear("adminnorte", "x", UserEntity.Role.ADMIN, empresa, List.of());
        Long otra = service.createCompany("Otra SA", YearMonth.of(2026, 9));
        service.createEstablishment(otra, "O1", "Campo Otro");
        userService.crear("adminotra", "x", UserEntity.Role.ADMIN, otra, List.of());
    }

    org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor admin() {
        return user("adminnorte").roles("ADMIN");
    }

    // ---- periodo: mes y año ----

    @Test
    void sinPeriodoSeMuestraElMesActualPreseleccionado() throws Exception {
        configurar();
        YearMonth ahora = YearMonth.now();
        mockMvc.perform(get("/planilla").param("establecimiento", "N1").with(admin()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"mesNumero\"")))
                .andExpect(content().string(containsString("name=\"anio\"")))
                .andExpect(content().string(containsString("<option value=\"" + ahora.getMonthValue() + "\" selected>")))
                .andExpect(content().string(containsString("<option value=\"" + ahora.getYear() + "\" selected>")))
                .andExpect(content().string(containsString("Descargar PDF")))
                .andExpect(content().string(containsString("/planilla/pdf?establecimiento=N1&mes=" + ahora)));
    }

    @Test
    void elMesYElAñoElegidosArmanElPeriodo() throws Exception {
        configurar();
        mockMvc.perform(get("/planilla").param("establecimiento", "N1").param("mesNumero", "9").param("anio", "2026").with(admin()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Campo Norte · septiembre 2026")))
                .andExpect(content().string(containsString("<option value=\"9\" selected>Septiembre</option>")))
                .andExpect(content().string(containsString("<option value=\"2026\" selected>2026</option>")))
                .andExpect(content().string(containsString("/planilla/pdf?establecimiento=N1&mes=2026-09")));
    }

    @Test
    void losEnlacesViejosConMesAaaaMmSiguenFuncionandoYUnAñoLejanoApareceEnElSelector() throws Exception {
        configurar();
        mockMvc.perform(get("/planilla").param("establecimiento", "N1").param("mes", "2015-03").with(admin()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<option value=\"3\" selected>Marzo</option>")))
                .andExpect(content().string(containsString("<option value=\"2015\" selected>2015</option>")));
    }

    @Test
    void unMesOAñoInvalidoCaeAlActualEnLugarDeRomperse() {
        YearMonth ahora = YearMonth.now();
        assertEquals(YearMonth.of(2026, ahora.getMonthValue()).toString(), PlanillaController.periodo(null, 99, 2026));
        assertEquals(YearMonth.of(Year.now().getValue(), 5).toString(), PlanillaController.periodo(null, 5, 12));
        assertEquals("2026-09", PlanillaController.periodo("2026-09", null, null));
        assertEquals(ahora.toString(), PlanillaController.periodo(null, null, null));
        assertEquals(ahora.toString(), PlanillaController.periodo("  ", null, null));
    }

    // ---- PDF ----

    @Test
    void elPdfSeDescargaConLosDatosDeLaPlanilla() throws Exception {
        configurar();
        byte[] pdf = mockMvc.perform(get("/planilla/pdf").param("establecimiento", "N1").param("mes", "2026-09").with(admin()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=planilla-N1-2026-09.pdf"))
                .andReturn().getResponse().getContentAsByteArray();
        assertTrue(new String(pdf, 0, 5, java.nio.charset.StandardCharsets.ISO_8859_1).startsWith("%PDF-"), "es un PDF");
        PdfReader lector = new PdfReader(pdf);
        String texto = new PdfTextExtractor(lector).getTextFromPage(1);
        assertTrue(texto.contains("Campo Norte - septiembre 2026"), texto);
        assertTrue(texto.contains("Vacunos"), texto);
        assertTrue(texto.contains("Toros"), texto);
        assertTrue(texto.contains("12"), "anterior 10 + compra 2 = final 12: " + texto);
        assertTrue(texto.contains("Total general: anterior 10, entradas 2, salidas 0, final 12"), texto);
        assertEquals(1, lector.getNumberOfPages());
        lector.close();
    }

    @Test
    void elPdfDeUnaPlanillaCerradaLoDice() throws Exception {
        configurar();
        service.closeMonth("N1", YearMonth.of(2026, 9));
        byte[] pdf = mockMvc.perform(get("/planilla/pdf").param("establecimiento", "N1").param("mes", "2026-09").with(admin()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        PdfReader lector = new PdfReader(pdf);
        assertTrue(new PdfTextExtractor(lector).getTextFromPage(1).contains("(cerrada)"));
        lector.close();
    }

    @Test
    void elPdfNoSeDescargaDeUnEstablecimientoAjenoNiConUnMesInvalido() throws Exception {
        configurar();
        mockMvc.perform(get("/planilla/pdf").param("establecimiento", "O1").param("mes", "2026-09").with(admin()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/planilla?establecimiento=O1&mes=2026-09&error=*"));
        mockMvc.perform(get("/planilla/pdf").param("establecimiento", "N1").param("mes", "no-es-mes").with(admin()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/planilla?establecimiento=N1&mes=no-es-mes&error=*"));
    }

    @Test
    void sinSesionElPdfPideIniciarSesion() throws Exception {
        configurar();
        mockMvc.perform(get("/planilla/pdf").param("establecimiento", "N1").param("mes", "2026-09"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("/login")));
    }
}
