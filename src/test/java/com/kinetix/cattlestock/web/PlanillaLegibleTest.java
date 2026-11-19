package com.kinetix.cattlestock.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

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
import com.kinetix.cattlestock.domain.Planilla;
import com.kinetix.cattlestock.domain.Species;

/** La planilla destaca lo que se movio: ceros apagados, grupos rotulados, y en el celular solo lo que tuvo movimiento. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PlanillaLegibleTest {

    @Autowired MockMvc mockMvc;
    @Autowired LedgerService service;
    @Autowired UserService userService;

    void configurar() {
        Long empresa = service.createCompany("Ganadera Norte", YearMonth.of(2026, 9));
        service.createEstablishment(empresa, "N1", "Campo Norte");
        service.createCategory(empresa, "toros", "Toros", Species.VACUNOS);
        service.createCategory(empresa, "vacas", "Vacas", Species.VACUNOS);
        service.setOpening("N1", "toros", 10);
        service.setOpening("N1", "vacas", 4);
        service.addMovement(Movement.compra("c1", LocalDate.of(2026, 9, 3), "N1", "toros", 2));
        userService.crear("adminnorte", "x", UserEntity.Role.ADMIN, empresa, List.of());
    }

    org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor admin() {
        return user("adminnorte").roles("ADMIN");
    }

    String planilla() throws Exception {
        return mockMvc.perform(get("/planilla").param("establecimiento", "N1").param("mes", "2026-09").with(admin()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    @Test
    void losCerosSeApaganYLosValoresNo() throws Exception {
        configurar();
        String html = planilla();
        assertTrue(html.contains("<td class=\"cero\">-</td>"), "los ceros de movimiento van como guion apagado");
        assertTrue(html.contains("<td class=\"\">2</td>"), "la compra de 2 queda como numero normal");
        assertTrue(html.contains("<strong>12</strong>"), "la existencia final sigue destacada");
    }

    @Test
    void lasColumnasSeAgrupanBajoEntradasYSalidasConAlcanceParaLectoresDePantalla() throws Exception {
        configurar();
        mockMvc.perform(get("/planilla").param("establecimiento", "N1").param("mes", "2026-09").with(admin()))
                .andExpect(content().string(containsString("<th scope=\"colgroup\" colspan=\"5\" class=\"ent\">Entradas</th>")))
                .andExpect(content().string(containsString("<th scope=\"colgroup\" colspan=\"7\" class=\"sal\">Salidas</th>")))
                .andExpect(content().string(containsString("<th scope=\"col\">Categoría</th>")))
                // un titulo por especie, a continuacion del h1 sin saltear niveles
                .andExpect(content().string(containsString("<h2 class=\"especie\">Vacunos</h2>")))
                .andExpect(content().string(not(containsString("<h3"))));
    }

    @Test
    void laLeyendaExplicaLasAbreviaturas() throws Exception {
        configurar();
        mockMvc.perform(get("/planilla").param("establecimiento", "N1").param("mes", "2026-09").with(admin()))
                .andExpect(content().string(containsString("<dt>Transf. DE</dt>")))
                .andExpect(content().string(containsString("salen por traslado hacia otro establecimiento")))
                .andExpect(content().string(containsString("<dt>Dif. recuento +/-</dt>")));
    }

    @Test
    void lasFilasYColumnasSinMovimientoSeMarcanParaOcultarlasEnElCelular() throws Exception {
        configurar();
        String html = planilla();
        // Vacas no se movio: se marca; Toros tuvo una compra: no
        assertTrue(Pattern.compile("<tr class=\"sin-mov\">\\s*<td>Vacas</td>").matcher(html).find(), "Vacas sin movimiento");
        assertTrue(Pattern.compile("<tr class=\"\">\\s*<td>Toros</td>").matcher(html).find(), "Toros con movimiento");
        // la unica columna de movimiento con datos es Compra (y el total Entradas): el resto se marca como vacia
        assertTrue(html.contains("<th scope=\"col\" class=\"ent\">Compra</th>"));
        assertTrue(html.contains("<th scope=\"col\" class=\"sal col-vacia\">Venta</th>"));
        assertTrue(html.contains("<th scope=\"col\" class=\"ent col-vacia\">Nacimiento</th>"));
        assertTrue(html.contains("<th scope=\"col\" class=\"sal col-vacia\">Salidas</th>"));
        // el interruptor para ver todo, con la cuenta de categorias ocultas
        assertTrue(html.contains("Mostrar las 1 categorías sin movimiento y las columnas vacías"));
        assertTrue(html.contains("id=\"ver-todas\""));
    }

    @Test
    void sinCategoriasQuietasNoSeOfreceElInterruptor() throws Exception {
        configurar();
        service.addMovement(Movement.compra("c2", LocalDate.of(2026, 9, 4), "N1", "vacas", 1));
        assertFalse(planilla().contains("id=\"ver-todas\""));
    }

    @Test
    void laVistaCalculaLasColumnasVaciasPorEspecie() {
        configurar();
        Planilla p = service.planilla("N1", YearMonth.of(2026, 9));
        boolean[] vacias = PlanillaVista.columnasVacias(p, Species.VACUNOS);
        assertEquals(14, vacias.length);
        assertFalse(vacias[0], "compra tuvo movimiento");
        assertTrue(vacias[1], "nacimiento no");
        assertFalse(vacias[12], "hubo entradas");
        assertTrue(vacias[13], "no hubo salidas");
        assertEquals(1, PlanillaVista.filasSinMovimiento(p));
    }

    @Test
    void laFilaTotalSumaLasEntradasYLasSalidasDeLaEspecieYElTotalGeneralLasMuestra() throws Exception {
        configurar();
        service.addMovement(Movement.venta("v1", LocalDate.of(2026, 9, 6), "N1", "toros", 3));
        String html = planilla();
        // Toros: +2 de compra y -3 de venta; Vacas sin movimiento. Total de Vacunos: entradas 2, salidas 3
        var fila = Pattern.compile("<td><strong>Total</strong></td>(.*?)</tr>", Pattern.DOTALL).matcher(html);
        assertTrue(fila.find(), "fila Total");
        String total = fila.group(1);
        assertTrue(total.contains("<td class=\"\">2</td>"), "total de entradas: " + total);
        assertTrue(total.contains("<td class=\"\">3</td>"), "total de salidas: " + total);
        assertTrue(html.contains("entradas <strong>2</strong>, salidas <strong>3</strong>"), "total general");
        Planilla p = service.planilla("N1", YearMonth.of(2026, 9));
        assertEquals(2, p.totalFor(Species.VACUNOS).entradas());
        assertEquals(3, p.totalFor(Species.VACUNOS).salidas());
        assertEquals(2, p.totalGeneral().entradas());
        assertEquals(3, p.totalGeneral().salidas());
    }

    // ---- tildes: el texto visible de las pantallas va bien escrito ----

    private static final Pattern SIN_TILDE = Pattern.compile(
            "(?<![\\w.=\\-/&\"])(Categoria|Categorias|Configuracion|Administracion|Rapida|Password|Periodo|Codigo|Elegi|Ingresa|Tambien|Despues)(?![\\w\\-=\"])");

    @Test
    void ningunaPantallaTraePalabrasDeUsoComunSinTilde() throws IOException {
        try (Stream<Path> archivos = Files.walk(Path.of("src/main/jte"))) {
            for (Path p : archivos.filter(f -> f.toString().endsWith(".jte")).toList()) {
                for (String linea : Files.readAllLines(p)) {
                    if (linea.stripLeading().startsWith("@param") || linea.stripLeading().startsWith("@import")) continue;
                    var m = SIN_TILDE.matcher(linea);
                    assertFalse(m.find(), p + " tiene una palabra sin tilde en: " + linea.strip());
                }
            }
        }
    }
}
