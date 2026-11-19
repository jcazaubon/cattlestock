package com.kinetix.cattlestock.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
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
import com.kinetix.cattlestock.domain.Species;

/** Ningun cuadro del navegador: las confirmaciones usan el dialogo de la app, y los botones de accion son iconos. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DialogosPropiosTest {

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
    }

    org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor admin() {
        return user("adminnorte").roles("ADMIN");
    }

    // ---- la guardia: nadie vuelve a usar un cuadro del navegador ----

    /** Los cuadros nativos del navegador: confirm, alert y prompt, sus atributos y los envios con onsubmit. */
    static final Pattern CUADROS_DEL_NAVEGADOR = Pattern.compile(
            "(?<![\\w.-])(window\\.)?(confirm|alert|prompt)\\s*\\(|hx-confirm|onsubmit\\s*=|beforeunload");

    @Test
    void ningunaPantallaNiScriptPropioUsaLosCuadrosDelNavegador() throws IOException {
        int revisados = 0;
        try (Stream<Path> archivos = Stream.concat(Files.walk(Path.of("src/main/jte")), Files.walk(Path.of("src/main/resources/static")))) {
            for (Path p : archivos.filter(Files::isRegularFile).toList()) {
                String nombre = p.getFileName().toString();
                if (nombre.equals("htmx.min.js")) continue; // libreria de terceros: trae su propio codigo de soporte
                if (!(nombre.endsWith(".jte") || nombre.endsWith(".js") || nombre.endsWith(".css") || nombre.endsWith(".html"))) continue;
                revisados++;
                var m = CUADROS_DEL_NAVEGADOR.matcher(Files.readString(p));
                assertTrue(!m.find(), p + " usa un cuadro del navegador: " + (m.hitEnd() ? "" : m.group()));
            }
        }
        assertTrue(revisados > 20, "se revisaron las pantallas y los scripts propios: " + revisados);
    }

    @Test
    void laGuardiaDetectaLosCuadrosNativosQueBusca() {
        for (String malo : List.of("return confirm('seguro?')", "window.alert(1)", "prompt ('nombre')",
                "<form onsubmit=\"x\">", "hx-confirm=\"seguro\"")) {
            assertTrue(CUADROS_DEL_NAVEGADOR.matcher(malo).find(), "deberia detectar: " + malo);
        }
        // y no se confunde con textos propios que se le parecen
        for (String bueno : List.of("data-confirmar=\"texto\"", "confirmado", "/configuracion/categorias/importar/confirmar",
                "Confirmar", "dialogo-confirmar")) {
            assertTrue(!CUADROS_DEL_NAVEGADOR.matcher(bueno).find(), "no deberia detectar: " + bueno);
        }
    }

    // ---- el dialogo propio ----

    @Test
    void elDialogoYSuScriptVienenEnLaBarraDeLasPantallasDeTrabajo() throws Exception {
        configurar();
        for (String ruta : List.of("/", "/planilla", "/historial", "/configuracion", "/existencia")) {
            mockMvc.perform(get(ruta).with(admin()))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("<dialog id=\"dialogo-confirmar\"")))
                    .andExpect(content().string(containsString("<script src=\"/confirmar.js\" defer>")))
                    .andExpect(content().string(containsString("data-rol=\"cancelar\"")))
                    .andExpect(content().string(containsString("data-rol=\"aceptar\"")));
        }
    }

    @Test
    void elScriptSeSirveYUsaElElementoDialogoDeLaPaginaSinCuadrosDelNavegador() throws Exception {
        configurar();
        mockMvc.perform(get("/confirmar.js").with(admin()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("showModal")))
                .andExpect(content().string(containsString("data-confirmar")))
                .andExpect(content().string(containsString("requestSubmit")))
                .andExpect(content().string(not(containsString("window.confirm"))));
    }

    @Test
    void cerrarUnMesEliminarUnMovimientoYDarDeBajaUnaCategoriaPidenConfirmacionConElDialogoDeLaApp() throws Exception {
        configurar();
        // cerrar la planilla
        mockMvc.perform(get("/planilla").param("establecimiento", "N1").param("mes", "2026-09").with(admin()))
                .andExpect(content().string(containsString("action=\"/planilla/cerrar\"")))
                .andExpect(content().string(containsString("data-confirmar-titulo=\"Cerrar la planilla de 2026-09\"")))
                .andExpect(content().string(containsString("data-confirmar-boton=\"Cerrar mes\"")))
                .andExpect(content().string(containsString("solo un administrador puede reabrirla")));
        // eliminar un movimiento, marcado como accion de peligro
        mockMvc.perform(get("/historial").param("mes", "2026-09").with(admin()))
                .andExpect(content().string(containsString("data-confirmar-titulo=\"Eliminar el movimiento\"")))
                .andExpect(content().string(containsString("data-confirmar-peligro=\"si\"")))
                .andExpect(content().string(containsString("Queda en el historial y se puede restaurar.")));
        // dar de baja una categoria, con su nombre en el titulo
        mockMvc.perform(get("/configuracion").param("seccion", "categorias").with(admin()))
                .andExpect(content().string(containsString("data-confirmar-titulo=\"Dar de baja Toros\"")))
                .andExpect(content().string(containsString("data-confirmar-boton=\"Dar de baja\"")));
    }

    // ---- botones de solo icono en la lista de categorias ----

    @Test
    void losBotonesDeAccionDeLasCategoriasSonSoloIconoConNombreAccesible() throws Exception {
        configurar();
        mockMvc.perform(get("/configuracion").param("seccion", "categorias").with(admin()))
                .andExpect(status().isOk())
                // cada accion es un icono con su tooltip y su nombre para lectores de pantalla
                .andExpect(content().string(containsString("title=\"Editar el nombre\" aria-label=\"Editar Toros\"><svg class=\"icono\"")))
                .andExpect(content().string(containsString("aria-label=\"Deshabilitar Toros\"><svg class=\"icono\"")))
                .andExpect(content().string(containsString("title=\"Dar de baja\" aria-label=\"Dar de baja Toros\"><svg class=\"icono\"")))
                .andExpect(content().string(containsString("aria-label=\"Subir Toros\"")))
                .andExpect(content().string(containsString("aria-label=\"Bajar Toros\"")))
                .andExpect(content().string(containsString("boton-icono")))
                // el icono es decorativo: el nombre ya lo da el boton
                .andExpect(content().string(containsString("aria-hidden=\"true\" focusable=\"false\"")))
                // y ya no hay botones con texto
                .andExpect(content().string(not(containsString(">Editar</button>"))))
                .andExpect(content().string(not(containsString(">Deshabilitar</button>"))))
                .andExpect(content().string(not(containsString(">Baja</button>"))))
                .andExpect(content().string(not(containsString("&uarr;"))))
                .andExpect(content().string(not(containsString("&darr;"))));
    }

    @Test
    void elIconoDeEstadoCambiaConLaAccionYLaEdicionTieneGuardarYCancelarComoIconos() throws Exception {
        configurar();
        service.setCategoryEnabled(service.companyOf("N1"), "toros", false);
        mockMvc.perform(get("/configuracion").param("seccion", "categorias").with(admin()))
                .andExpect(content().string(containsString("aria-label=\"Habilitar Toros\"><svg class=\"icono\"")))
                .andExpect(content().string(not(containsString("aria-label=\"Deshabilitar Toros\""))));
        mockMvc.perform(get("/configuracion").param("seccion", "categorias").param("editar", "toros").with(admin()))
                .andExpect(content().string(containsString("title=\"Guardar nombre\" aria-label=\"Guardar nombre\">")))
                .andExpect(content().string(containsString("title=\"Cancelar\" aria-label=\"Cancelar\">")))
                .andExpect(content().string(not(containsString(">Guardar</button>"))))
                // (el dialogo de la barra si tiene su propio Cancelar con texto: se acota al boton viejo de la fila)
                .andExpect(content().string(not(containsString("boton-linea\">Cancelar</button>"))));
    }
}
