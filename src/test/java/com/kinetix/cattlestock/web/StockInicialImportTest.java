package com.kinetix.cattlestock.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.kinetix.cattlestock.app.LedgerService;
import com.kinetix.cattlestock.app.UserService;
import com.kinetix.cattlestock.data.UserEntity;
import com.kinetix.cattlestock.domain.DomainException;
import com.kinetix.cattlestock.domain.Movement;
import com.kinetix.cattlestock.domain.Species;

/** Cargar el stock inicial desde un Excel: descargar la planilla, completarla, ver lo que cambia y aplicarlo. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StockInicialImportTest {

    static final YearMonth SEP = YearMonth.of(2026, 9);
    static final LocalDate FIN = LocalDate.of(2026, 9, 30);
    static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Autowired MockMvc mockMvc;
    @Autowired LedgerService service;
    @Autowired UserService userService;

    Long norte;
    Long sur;

    void configurar() {
        norte = service.createCompany("Ganadera Norte", SEP);
        service.createEstablishment(norte, "N1", "Campo Norte");
        service.createEstablishment(norte, "N2", "Campo Dos");
        service.createCategory(norte, "toros", "Toros", Species.VACUNOS);
        service.createCategory(norte, "vacas", "Vacas", Species.VACUNOS);
        service.setOpening("N1", "toros", 10);

        sur = service.createCompany("Ganadera Sur", SEP);
        service.createEstablishment(sur, "S1", "Campo Sur");
        service.createCategory(sur, "toros", "Toros", Species.VACUNOS);
        service.setOpening("S1", "toros", 77);

        userService.crear("adminnorte", "x", UserEntity.Role.ADMIN, norte, List.of());
        userService.crear("pepe", "x", UserEntity.Role.CARGA, norte, List.of("N1"));
    }

    byte[] descargar() throws Exception {
        return mockMvc.perform(get("/configuracion/stock/plantilla").with(user("adminnorte").roles("ADMIN")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
    }

    byte[] completar(byte[] plantilla, int fila, int columna, double valor) throws Exception {
        Workbook libro = WorkbookFactory.create(new ByteArrayInputStream(plantilla));
        Sheet hoja = libro.getSheet("Stock inicial");
        hoja.getRow(fila).getCell(columna).setCellValue(valor);
        try (libro; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            libro.write(out);
            return out.toByteArray();
        }
    }

    int stock(String est, String cat) {
        Long empresa = service.companyOf(est);
        return service.existencia(empresa, FIN, est, null, cat).get(0).quantity();
    }

    @Test
    void laPlantillaSeDescargaConLasCategoriasDeLaEmpresaYLoYaCargadoYNoMezclaOtrasEmpresas() throws Exception {
        configurar();
        mockMvc.perform(get("/configuracion/stock/plantilla").with(user("adminnorte").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString(XLSX)))
                .andExpect(header().string("Content-Disposition", containsString("stock-inicial-ganadera-norte.xlsx")));
        try (Workbook libro = WorkbookFactory.create(new ByteArrayInputStream(descargar()))) {
            Sheet hoja = libro.getSheet("Stock inicial");
            assertEquals("Campo Norte (N1)", hoja.getRow(0).getCell(1).getStringCellValue());
            assertEquals("Campo Dos (N2)", hoja.getRow(0).getCell(2).getStringCellValue());
            assertEquals("Toros", hoja.getRow(1).getCell(0).getStringCellValue());
            assertEquals(10, hoja.getRow(1).getCell(1).getNumericCellValue(), "trae el stock ya cargado");
            assertEquals(3, hoja.getLastRowNum() + 1, "encabezado y las 2 categorias de Norte, ninguna de Sur");
            assertEquals(3, hoja.getRow(0).getLastCellNum(), "solo los 2 establecimientos de Norte");
        }
    }

    @Test
    void soloLoDescargaQuienAdministraLaEmpresa() throws Exception {
        configurar();
        mockMvc.perform(get("/configuracion/stock/plantilla").with(user("pepe").roles("CARGA")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/configuracion").param("seccion", "stock").with(user("pepe").roles("CARGA")))
                .andExpect(status().isForbidden());
    }

    @Test
    void completarSubirVerLaVistaPreviaYAplicar() throws Exception {
        configurar();
        byte[] archivo = completar(completar(descargar(), 1, 1, 120), 2, 2, 35); // Toros N1 = 120, Vacas N2 = 35
        var excel = new MockMultipartFile("archivo", "stock.xlsx", XLSX, archivo);

        // 1) vista previa: lo que hay hoy, lo nuevo y que no se aplico nada todavia
        mockMvc.perform(multipart("/configuracion/stock/importar").file(excel)
                        .with(user("adminnorte").roles("ADMIN")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Importar stock inicial")))
                .andExpect(content().string(containsString("El archivo trae 2 valores y 2 cambian.")))
                .andExpect(content().string(containsString("name=\"celda\" value=\"N1|toros|120\"")))
                .andExpect(content().string(containsString("name=\"celda\" value=\"N2|vacas|35\"")))
                .andExpect(content().string(containsString("Aplicar 2 cambios")));
        assertEquals(10, stock("N1", "toros"), "la vista previa no cambia nada");

        // 2) confirmar: se aplica, y la planilla muestra lo nuevo
        mockMvc.perform(post("/configuracion/stock/importar/confirmar")
                        .with(user("adminnorte").roles("ADMIN")).with(csrf())
                        .param("celda", "N1|toros|120", "N2|vacas|35"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/configuracion?seccion=stock&aviso=Se+actualizaron+2+valores+de+stock+inicial."));
        assertEquals(120, stock("N1", "toros"));
        assertEquals(35, stock("N2", "vacas"));
        assertEquals(77, stock("S1", "toros"), "otra empresa no se toca");
    }

    @Test
    void laPantallaDeStockExplicaLosPasosYLaVistaPreviaAvisaLoQueIgnoro() throws Exception {
        configurar();
        mockMvc.perform(get("/configuracion").param("seccion", "stock").with(user("adminnorte").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Descargar planilla de Excel")))
                .andExpect(content().string(containsString("Trae las 2 categorías de la empresa")))
                .andExpect(content().string(containsString("aria-current=\"page\">Stock inicial")));
        // una categoria inventada y un valor valido: se importa el valido y se avisa del otro
        byte[] archivo = descargar();
        var libro = WorkbookFactory.create(new ByteArrayInputStream(archivo));
        var hoja = libro.getSheet("Stock inicial");
        hoja.getRow(1).getCell(1).setCellValue(15);
        hoja.createRow(4).createCell(0).setCellValue("Dinosaurios");
        hoja.getRow(4).createCell(1).setCellValue(3);
        var out = new ByteArrayOutputStream();
        libro.write(out);
        libro.close();
        mockMvc.perform(multipart("/configuracion/stock/importar")
                        .file(new MockMultipartFile("archivo", "s.xlsx", XLSX, out.toByteArray()))
                        .with(user("adminnorte").roles("ADMIN")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Se ignoro 1 cosa del archivo")))
                .andExpect(content().string(containsString("Dinosaurios")))
                .andExpect(content().string(containsString("value=\"N1|toros|15\"")));
    }

    @Test
    void sinArchivoOSinCantidadesVuelveConUnAviso() throws Exception {
        configurar();
        mockMvc.perform(multipart("/configuracion/stock/importar")
                        .with(user("adminnorte").roles("ADMIN")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/configuracion?seccion=stock&error=Eleg%C3%AD+el+archivo+de+Excel+con+el+stock"));
        // la planilla tal como se bajo, sin completar nada nuevo, no es un error: trae lo ya cargado (10)
        mockMvc.perform(multipart("/configuracion/stock/importar")
                        .file(new MockMultipartFile("archivo", "s.xlsx", XLSX, descargar()))
                        .with(user("adminnorte").roles("ADMIN")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("El archivo trae 1 valor y 0 cambian.")));
        mockMvc.perform(multipart("/configuracion/stock/importar")
                        .file(new MockMultipartFile("archivo", "roto.xlsx", XLSX, "nada".getBytes()))
                        .with(user("adminnorte").roles("ADMIN")).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/configuracion?seccion=stock&error=No+pude+leer")));
    }

    @Test
    void aplicarEsTodoONadaYNoCruzaDeEmpresa() throws Exception {
        configurar();
        // un valor invalido entre los validos: no se aplica ninguno
        assertThrows(DomainException.class, () -> service.importOpenings(norte, List.of(
                new LedgerService.NuevaApertura("N1", "toros", 50),
                new LedgerService.NuevaApertura("N2", "vacas", -1))));
        assertEquals(10, stock("N1", "toros"), "no quedo a medias");
        // un establecimiento de otra empresa se rechaza aunque el resto sea valido
        var e = assertThrows(DomainException.class, () -> service.importOpenings(norte, List.of(
                new LedgerService.NuevaApertura("N1", "toros", 50),
                new LedgerService.NuevaApertura("S1", "toros", 1))));
        assertTrue(e.getMessage().contains("S1"), e.getMessage());
        assertEquals(10, stock("N1", "toros"));
        assertEquals(77, stock("S1", "toros"));
        // y por la pantalla: el admin de Norte no puede mandar una celda de Sur
        mockMvc.perform(post("/configuracion/stock/importar/confirmar")
                        .with(user("adminnorte").roles("ADMIN")).with(csrf())
                        .param("celda", "N1|toros|50", "S1|toros|1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("seccion=stock&error=")));
        assertEquals(10, stock("N1", "toros"));
        assertEquals(77, stock("S1", "toros"));
    }

    @Test
    void respetaLasReglasDelDominioComoLaCargaManual() throws Exception {
        configurar();
        // hay una venta de 8 en el mes: dejar el stock inicial en 5 dejaria el saldo negativo
        service.addMovement(Movement.venta("v1", LocalDate.of(2026, 9, 10), "N1", "toros", 8));
        var e = assertThrows(DomainException.class, () -> service.importOpenings(norte, List.of(
                new LedgerService.NuevaApertura("N1", "toros", 5))));
        assertTrue(e.getMessage().toLowerCase().contains("stock insuficiente"), e.getMessage());
        assertEquals(2, stock("N1", "toros"), "quedo como estaba");
        // subirlo si se puede, y no cuenta como cambio lo que ya valia lo mismo
        assertEquals(1, service.importOpenings(norte, List.of(
                new LedgerService.NuevaApertura("N1", "toros", 30),
                new LedgerService.NuevaApertura("N2", "vacas", 0)))
                - 1 /* el N2 vacas pasa de "sin cargar" a 0: cuenta como cambio */);
        assertEquals(22, stock("N1", "toros"));
        assertEquals(0, service.importOpenings(norte, List.of(new LedgerService.NuevaApertura("N1", "toros", 30))));
    }
}
