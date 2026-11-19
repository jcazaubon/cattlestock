package com.kinetix.cattlestock.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.kinetix.cattlestock.app.LedgerService;
import com.kinetix.cattlestock.app.MigracionEmpresas;
import com.kinetix.cattlestock.app.UserService;
import com.kinetix.cattlestock.data.CompanyEntity;
import com.kinetix.cattlestock.data.CompanyRepository;
import com.kinetix.cattlestock.data.EstablishmentEntity;
import com.kinetix.cattlestock.data.EstablishmentRepository;
import com.kinetix.cattlestock.data.UserEntity;
import com.kinetix.cattlestock.data.UserRepository;
import com.kinetix.cattlestock.domain.DomainException;
import com.kinetix.cattlestock.domain.Movement;
import com.kinetix.cattlestock.domain.Species;

/** Varias empresas en la misma instalacion: aislamiento, roles y panel del superadmin. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MultiempresaTest {

    static final YearMonth SEP = YearMonth.of(2026, 9);

    @Autowired MockMvc mockMvc;
    @Autowired LedgerService service;
    @Autowired UserService userService;
    @Autowired CompanyRepository companies;
    @Autowired EstablishmentRepository establishments;
    @Autowired UserRepository users;
    @Autowired MigracionEmpresas migracion;
    @Autowired com.kinetix.cattlestock.app.MigracionCategorias categorias;
    @Autowired javax.sql.DataSource dataSource;

    Long norte;
    Long sur;

    /** Dos empresas con un establecimiento cada una, un superadmin, un admin por empresa y uno de carga. */
    void configurar() {
        norte = service.createCompany("Ganadera Norte", SEP);
        service.createEstablishment(norte, "N1", "Campo Norte");
        service.createCategory(service.companyOf("N1"), "toros", "Toros", Species.VACUNOS);
        service.setOpening("N1", "toros", 10);

        sur = service.createCompany("Ganadera Sur", SEP);
        service.createEstablishment(sur, "S1", "Campo Sur");
        service.createCategory(service.companyOf("S1"), "vacas", "Vacas", Species.VACUNOS);
        service.setOpening("S1", "vacas", 77);

        userService.crearSuperadmin("root", "root");
        userService.crear("adminnorte", "x", UserEntity.Role.ADMIN, norte, List.of());
        userService.crear("adminsur", "x", UserEntity.Role.ADMIN, sur, List.of());
        userService.crear("pepe", "x", UserEntity.Role.CARGA, norte, List.of("N1"));
    }

    @Test
    void cadaEmpresaTieneSuPropioLedgerYNoSeMezclan() {
        configurar();
        assertEquals(10, service.existencia(norte, LocalDate.of(2026, 9, 30), null, null, null).get(0).quantity());
        assertEquals(1, service.existencia(norte, LocalDate.of(2026, 9, 30), null, null, null).size());
        assertEquals(77, service.existencia(sur, LocalDate.of(2026, 9, 30), null, null, null).get(0).quantity());
        // pedir a una empresa un establecimiento de la otra no devuelve nada ajeno: falla
        assertThrows(DomainException.class,
                () -> service.existencia(norte, LocalDate.of(2026, 9, 30), "S1", null, null));
    }

    @Test
    void unTrasladoNoCruzaDeEmpresa() {
        configurar();
        DomainException e = assertThrows(DomainException.class, () -> service.addMovement(
                Movement.traslado("t1", LocalDate.of(2026, 9, 5), "N1", "toros", "S1", "vacas", 2)));
        assertTrue(e.getMessage().contains("S1"), e.getMessage());
        assertEquals(10, service.existencia(norte, LocalDate.of(2026, 9, 30), "N1", null, null).get(0).quantity());
    }

    @Test
    void laEmpresaNoSeRepiteYElCodigoDelEstablecimientoSeGeneraSolo() {
        configurar();
        assertThrows(DomainException.class, () -> service.createCompany("ganadera norte", SEP));
        assertEquals("lamarta", service.codigoDisponible("La Marta"));
        assertEquals("campo", service.codigoDisponible("Campo"));
        service.createEstablishment(norte, "campo", "Campo");
        assertEquals("campo2", service.codigoDisponible("Campo"));
    }

    @Test
    void elAdminDeUnaEmpresaSoloVeLosEstablecimientosDeLaSuya() throws Exception {
        configurar();
        mockMvc.perform(get("/").with(user("adminnorte").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Campo Norte")))
                .andExpect(content().string(not(containsString("Campo Sur"))))
                .andExpect(content().string(containsString("Ganadera Norte")));
        mockMvc.perform(get("/planilla").param("establecimiento", "S1").param("mes", "2026-09")
                        .with(user("adminnorte").roles("ADMIN")))
                .andExpect(content().string(not(containsString("<strong>77</strong>"))));
    }

    @Test
    void elAdminNoPuedeConfigurarNiAsignarNadaDeOtraEmpresa() throws Exception {
        configurar();
        // las categorias van al catalogo de la empresa del admin, venga el establecimiento que venga
        mockMvc.perform(post("/configuracion/categorias").with(user("adminnorte").roles("ADMIN")).with(csrf())
                        .param("establecimiento", "S1").param("codigo", "x").param("nombre", "X")
                        .param("especie", "VACUNOS"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/configuracion?seccion=categorias"));
        assertEquals(1, service.categoriasDeEmpresa(sur).size(), "el catalogo de Sur no cambia");
        assertEquals(2, service.categoriasDeEmpresa(norte).size());
        // el stock inicial de un establecimiento ajeno tampoco se toca
        mockMvc.perform(post("/configuracion/stock").with(user("adminnorte").roles("ADMIN")).with(csrf())
                        .param("establecimiento", "S1").param("stock_vacas", "5"))
                .andExpect(status().is3xxRedirection());
        assertEquals(77, service.existencia(sur, LocalDate.of(2026, 9, 30), "S1", null, null).get(0).quantity());
        // usuario con un establecimiento de otra empresa
        mockMvc.perform(post("/configuracion/usuarios").with(user("adminnorte").roles("ADMIN")).with(csrf())
                        .param("username", "intruso").param("password", "x").param("rol", "CARGA")
                        .param("establecimientos", "S1"))
                .andExpect(status().is3xxRedirection());
        assertTrue(users.findByUsername("intruso").isEmpty());
        // y no puede crear superadmins por la puerta de atras
        mockMvc.perform(post("/configuracion/usuarios").with(user("adminnorte").roles("ADMIN")).with(csrf())
                        .param("username", "jefe").param("password", "x").param("rol", "SUPERADMIN"))
                .andExpect(status().is3xxRedirection());
        assertTrue(users.findByUsername("jefe").isEmpty());
    }

    @Test
    void elAdminCreaUsuariosEnSuEmpresaYLesAsignaRol() throws Exception {
        configurar();
        mockMvc.perform(post("/configuracion/usuarios").with(user("adminnorte").roles("ADMIN")).with(csrf())
                        .param("username", "lucia").param("password", "x").param("rol", "CARGA")
                        .param("establecimientos", "N1"))
                .andExpect(status().is3xxRedirection());
        UserEntity lucia = users.findByUsername("lucia").orElseThrow();
        assertEquals(norte, lucia.getCompanyId());
        assertEquals(UserEntity.Role.CARGA, lucia.getRole());

        mockMvc.perform(post("/configuracion/usuarios/" + lucia.getId()).with(user("adminnorte").roles("ADMIN")).with(csrf())
                        .param("rol", "ADMIN"))
                .andExpect(status().is3xxRedirection());
        assertEquals(UserEntity.Role.ADMIN, users.findById(lucia.getId()).orElseThrow().getRole());

        // un usuario de otra empresa no se puede tocar
        UserEntity delSur = users.findByUsername("adminsur").orElseThrow();
        mockMvc.perform(post("/configuracion/usuarios/" + delSur.getId()).with(user("adminnorte").roles("ADMIN")).with(csrf())
                        .param("rol", "CARGA"))
                .andExpect(status().is3xxRedirection());
        assertEquals(UserEntity.Role.ADMIN, users.findById(delSur.getId()).orElseThrow().getRole());
    }

    @Test
    void elAdminEditaElNombreDeSuEstablecimientoPeroNoElDeOtraEmpresa() throws Exception {
        configurar();
        mockMvc.perform(post("/configuracion/establecimientos/N1").with(user("adminnorte").roles("ADMIN")).with(csrf())
                        .param("nombre", "Campo Norte Grande"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/configuracion?establecimiento=N1"));
        assertEquals("Campo Norte Grande", establishments.findById("N1").orElseThrow().getName());
        // el codigo y los datos que cuelgan de el siguen igual
        assertEquals(10, service.existencia(norte, LocalDate.of(2026, 9, 30), "N1", null, null).get(0).quantity());
        mockMvc.perform(get("/configuracion").param("establecimiento", "N1").with(user("adminnorte").roles("ADMIN")))
                .andExpect(content().string(containsString("panel-detalle")))
                .andExpect(content().string(containsString("Stock inicial")))
                .andExpect(content().string(containsString("value=\"Campo Norte Grande\"")));

        // un establecimiento de otra empresa no se toca
        mockMvc.perform(post("/configuracion/establecimientos/S1").with(user("adminnorte").roles("ADMIN")).with(csrf())
                        .param("nombre", "Hackeado"))
                .andExpect(status().is3xxRedirection());
        assertEquals("Campo Sur", establishments.findById("S1").orElseThrow().getName());

        // sin nombre vuelve con aviso, y no se repite el de otro establecimiento de la misma empresa
        service.createEstablishment(norte, "N2", "Campo Dos");
        assertThrows(DomainException.class, () -> service.renameEstablishment("N2", " campo norte grande "));
        assertThrows(DomainException.class, () -> service.renameEstablishment("N2", "  "));
        assertEquals("Campo Dos", establishments.findById("N2").orElseThrow().getName());
    }

    @Test
    void laConfiguracionListaLasCategoriasDeLaEmpresaYGeneraElCodigoSiNoSeEscribe() throws Exception {
        configurar();
        // sin codigo: se genera a partir del nombre ("Novillos Gordos" da "novillosgordos")
        mockMvc.perform(post("/configuracion/categorias").with(user("adminnorte").roles("ADMIN")).with(csrf())
                        .param("establecimiento", "N1").param("nombre", "Novillos Gordos")
                        .param("especie", "VACUNOS"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/configuracion?seccion=categorias"));
        assertEquals("novillosgordos2", service.codigoCategoriaDisponible(norte, "Novillos Gordos"));

        mockMvc.perform(get("/configuracion").param("seccion", "categorias").with(user("adminnorte").roles("ADMIN")))
                .andExpect(status().isOk())
                // el listado trae las categorias de la empresa con su establecimiento y codigo
                .andExpect(content().string(containsString("listado-categorias")))
                .andExpect(content().string(containsString("<td>Novillos Gordos</td>")))
                .andExpect(content().string(containsString("<td class=\"codigo\">novillosgordos</td>")))
                .andExpect(content().string(containsString("<td>Toros</td>")))
                // y no las de otra empresa
                .andExpect(content().string(not(containsString("<td>Vacas</td>"))))
                // las altas van en una linea (formulario de filtros) y no apiladas
                .andExpect(content().string(containsString("class=\"filtros\"")));
    }

    @Test
    void importarCategoriasDesdeUnExcelPasaPorUnaVistaPreviaYSoloCreaLoTildado() throws Exception {
        configurar();
        byte[] excel = com.kinetix.cattlestock.app.ImportadorCategoriasTest.bytes(
                com.kinetix.cattlestock.app.ImportadorCategoriasTest.formatoSalidaEntrada());
        var archivo = new org.springframework.mock.web.MockMultipartFile("archivo", "mulita.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", excel);
        // 1) vista previa: se detecta lo que hay, con la especie adivinada y lo que ya existe marcado
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .multipart("/configuracion/categorias/importar").file(archivo)
                        .with(user("adminnorte").roles("ADMIN")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Importar categorías")))
                .andExpect(content().string(containsString("la hoja \"NOVIEMBRE 2026\"")))
                .andExpect(content().string(containsString("value=\"Toros Braman\"")))
                .andExpect(content().string(containsString("value=\"Vaquillonas 1er servicio\"")))
                .andExpect(content().string(containsString("<option value=\"YEGUARIZOS\" selected")));
        assertEquals(1, service.categoriasDeEmpresa(norte).size(), "la vista previa no crea nada");

        // 2) confirmar: solo las tildadas (0 y 2), con el nombre corregido
        mockMvc.perform(post("/configuracion/categorias/importar/confirmar")
                        .with(user("adminnorte").roles("ADMIN")).with(csrf())
                        .param("nombre", "Toros Braman", "Vacas Cria", "Vaquillonas Primer Servicio", "Yeguarizos")
                        .param("especie", "VACUNOS", "VACUNOS", "VACUNOS", "YEGUARIZOS")
                        .param("incluir", "0", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/configuracion?seccion=categorias&aviso=Se+importaron+2+categorias."));
        var nombres = service.categoriasDeEmpresa(norte).stream().map(c -> c.getName()).toList();
        assertEquals(List.of("Toros", "Toros Braman", "Vaquillonas Primer Servicio"), nombres);
        assertEquals(1, service.categoriasDeEmpresa(sur).size(), "otra empresa no se entera");

        // 3) pegar texto y nada que importar
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .multipart("/configuracion/categorias/importar").param("texto", "Terneros\nTerneras")
                        .with(user("adminnorte").roles("ADMIN")).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"Terneras\"")));
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .multipart("/configuracion/categorias/importar")
                        .with(user("adminnorte").roles("ADMIN")).with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void laMigracionJuntaLasCategoriasDeCadaEstablecimientoEnElCatalogoDeLaEmpresa() throws Exception {
        var jdbc = new org.springframework.jdbc.core.JdbcTemplate(dataSource);
        // una base de la version anterior: la tabla de categorias tenia la columna establishment_code.
        // El ALTER va primero (confirma la transaccion en H2) y no se deshace: la columna nullable no molesta
        // a las otras pruebas, y asi los datos de esta se siguen revirtiendo al final.
        jdbc.execute("alter table category add column if not exists establishment_code varchar(255)");
        configurar();
        service.createEstablishment(norte, "N2", "Campo Dos");
        establishments.flush(); // el JDBC de abajo tiene que ver los establecimientos recien creados
        jdbc.update("delete from category where company_id = ?", norte);
        jdbc.update("insert into category (id, code, name, species, position, establishment_code) values (9001, 'toros', 'Toros', 'VACUNOS', 0, 'N1')");
        jdbc.update("insert into category (id, code, name, species, position, establishment_code) values (9002, 'toros', 'Toros', 'VACUNOS', 0, 'N2')");
        jdbc.update("insert into category (id, code, name, species, position, establishment_code) values (9003, 'vacas', 'Vacas', 'VACUNOS', 1, 'N2')");

        categorias.run(new DefaultApplicationArguments());

        assertEquals(List.of("Toros", "Vacas"), service.categoriasDeEmpresa(norte).stream().map(c -> c.getName()).toList(),
                "toros en dos establecimientos queda una sola vez");
        assertEquals(1, service.categoriasDeEmpresa(sur).size(), "la otra empresa no se toca");
        // y es idempotente
        categorias.run(new DefaultApplicationArguments());
        assertEquals(2, service.categoriasDeEmpresa(norte).size());
    }

    @Test
    void laAsignacionDeEstablecimientosTambienLimitaAlAdministrador() throws Exception {
        configurar();
        service.createEstablishment(norte, "N2", "Campo Dos");
        service.setOpening("N2", "toros", 7);
        userService.crear("adminparcial", "x", UserEntity.Role.ADMIN, norte, List.of("N1"));

        // con un establecimiento tildado, solo ve ese: en la portada, en la planilla y en la existencia
        mockMvc.perform(get("/").with(user("adminparcial").roles("ADMIN")))
                .andExpect(content().string(containsString("Campo Norte")))
                .andExpect(content().string(not(containsString("Campo Dos"))));
        mockMvc.perform(get("/planilla").param("establecimiento", "N2").param("mes", "2026-09")
                        .with(user("adminparcial").roles("ADMIN")))
                .andExpect(content().string(not(containsString("<strong>7</strong>"))));
        mockMvc.perform(get("/existencia").param("fecha", "2026-09-30").with(user("adminparcial").roles("ADMIN")))
                .andExpect(content().string(containsString("animales al 30/09/2026")))
                .andExpect(content().string(containsString(">10<")))
                .andExpect(content().string(not(containsString("17 animales"))));

        // sin ninguno tildado ve todos (el admin comun de la empresa)
        mockMvc.perform(get("/").with(user("adminnorte").roles("ADMIN")))
                .andExpect(content().string(containsString("Campo Norte")))
                .andExpect(content().string(containsString("Campo Dos")));

        // pero en Configuración el administrador parcial ve todos, para poder administrarlos
        mockMvc.perform(get("/configuracion").with(user("adminparcial").roles("ADMIN")))
                .andExpect(content().string(containsString("Campo Norte")))
                .andExpect(content().string(containsString("Campo Dos")));
        // y puede cargar stock en uno que no opera, porque lo configura
        mockMvc.perform(post("/configuracion/stock").with(user("adminparcial").roles("ADMIN")).with(csrf())
                        .param("establecimiento", "N2").param("stock_toros", "9"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/configuracion?establecimiento=N2"));
        assertEquals(9, service.existencia(norte, LocalDate.of(2026, 9, 30), "N2", null, null).get(0).quantity());
    }

    @Test
    void laConfiguracionSeOrganizaEnPestanasYMuestraSoloLaActiva() throws Exception {
        configurar();
        var admin = user("adminnorte").roles("ADMIN");
        // por defecto: establecimientos; la pestana activa se marca y trae su contenido, no el de las otras
        mockMvc.perform(get("/configuracion").with(admin))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"pestanas\"")))
                .andExpect(content().string(containsString("href=\"/configuracion?seccion=establecimientos\" aria-current=\"page\"")))
                .andExpect(content().string(containsString("Agregar establecimiento")))
                .andExpect(content().string(not(containsString("Agregar categoría"))))
                .andExpect(content().string(not(containsString("Agregar usuario"))));
        mockMvc.perform(get("/configuracion").param("seccion", "categorias").with(admin))
                .andExpect(content().string(containsString("Agregar categoría")))
                .andExpect(content().string(containsString("Importar desde una planilla o un texto")))
                .andExpect(content().string(not(containsString("Agregar establecimiento"))));
        mockMvc.perform(get("/configuracion").param("seccion", "usuarios").with(admin))
                .andExpect(content().string(containsString("Agregar usuario")))
                .andExpect(content().string(containsString("Rol y establecimientos")));
        // los contadores de las pestanas son datos reales y una seccion desconocida cae en la primera
        mockMvc.perform(get("/configuracion").param("seccion", "inventada").with(admin))
                .andExpect(content().string(containsString("Agregar establecimiento")));
        // editar un establecimiento lleva a su pestana aunque se pida otra
        mockMvc.perform(get("/configuracion").param("seccion", "usuarios").param("establecimiento", "N1").with(admin))
                .andExpect(content().string(containsString("panel-detalle")));
        // y cada accion vuelve a la pestana donde se hizo
        mockMvc.perform(post("/configuracion/usuarios").with(admin).with(csrf())
                        .param("username", "").param("password", "x").param("rol", "CARGA"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/configuracion?seccion=usuarios&error=Escribi+el+usuario"));
    }

    @Test
    void laEmpresaNoSeQuedaSinAdministrador() {
        configurar();
        UserEntity unico = users.findByUsername("adminnorte").orElseThrow();
        DomainException e = assertThrows(DomainException.class,
                () -> userService.actualizar(unico.getId(), norte, UserEntity.Role.CARGA, List.of()));
        assertTrue(e.getMessage().contains("al menos un administrador"), e.getMessage());
    }

    @Test
    void soloElSuperadminEntraAlPanelDeAdministracion() throws Exception {
        configurar();
        mockMvc.perform(get("/admin").with(user("adminnorte").roles("ADMIN"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin").with(user("pepe").roles("CARGA"))).andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/empresas").with(user("adminnorte").roles("ADMIN")).with(csrf())
                        .param("nombre", "X").param("mesInicial", "2026-09")
                        .param("adminUsuario", "x").param("adminPassword", "x"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/admin").with(user("root").roles("SUPERADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ganadera Norte")))
                .andExpect(content().string(containsString("Ganadera Sur")));
    }

    @Test
    void elSuperadminDaDeAltaUnaEmpresaConSuAdministradorYEntraAElla() throws Exception {
        configurar();
        mockMvc.perform(post("/admin/empresas").with(user("root").roles("SUPERADMIN")).with(csrf())
                        .param("nombre", "Ganadera Este").param("mesInicial", "2026-10")
                        .param("adminUsuario", "adminestes").param("adminPassword", "secreto"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));
        CompanyEntity este = companies.findAllByOrderByNameAsc().stream()
                .filter(c -> c.getName().equals("Ganadera Este")).findFirst().orElseThrow();
        UserEntity admin = users.findByUsername("adminestes").orElseThrow();
        assertEquals(UserEntity.Role.ADMIN, admin.getRole());
        assertEquals(este.getId(), admin.getCompanyId());

        // un usuario repetido vuelve al panel con el aviso
        mockMvc.perform(post("/admin/empresas").with(user("root").roles("SUPERADMIN")).with(csrf())
                        .param("nombre", "Ganadera Oeste").param("mesInicial", "2026-10")
                        .param("adminUsuario", "adminestes").param("adminPassword", "otro"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin?error=El+usuario+adminestes+ya+existe"));

        // el superadmin cambia de empresa y ve los establecimientos de esa, no los de la anterior
        var sesion = new org.springframework.mock.web.MockHttpSession();
        mockMvc.perform(post("/admin/empresas/" + sur + "/entrar").session(sesion)
                        .with(user("root").roles("SUPERADMIN")).with(csrf()))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/").session(sesion).with(user("root").roles("SUPERADMIN")))
                .andExpect(content().string(containsString("Campo Sur")))
                .andExpect(content().string(not(containsString("Campo Norte"))));
        mockMvc.perform(get("/").session(sesion).with(user("root").roles("SUPERADMIN")))
                .andExpect(content().string(containsString("Ganadera Sur")));
    }

    @Test
    void elNombreDeLaEmpresaEnLaBarraEsUnSelectorSoloParaElSuperadmin() throws Exception {
        configurar();
        // el superadmin tiene la lista de empresas en la barra, con la activa marcada
        mockMvc.perform(get("/").with(user("root").roles("SUPERADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"menu-empresa\"")))
                .andExpect(content().string(containsString("action=\"/admin/empresas/" + norte + "/entrar\"")))
                .andExpect(content().string(containsString("action=\"/admin/empresas/" + sur + "/entrar\"")))
                .andExpect(content().string(containsString("class=\"activa\">Ganadera Norte</button>")));
        // el administrador ve su empresa como texto: no hay otra a la que pasar ni el nombre de las demas
        mockMvc.perform(get("/").with(user("adminnorte").roles("ADMIN")))
                .andExpect(content().string(containsString("<span class=\"empresa\">Ganadera Norte</span>")))
                .andExpect(content().string(not(containsString("menu-empresa\""))))
                .andExpect(content().string(not(containsString("Ganadera Sur"))));
    }

    @Test
    void unAdminNoPuedeEntrarAOtraEmpresaConElSelector() throws Exception {
        configurar();
        mockMvc.perform(post("/admin/empresas/" + sur + "/entrar").with(user("adminnorte").roles("ADMIN")).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void laMigracionPasaLaEmpresaVieja_aPruebaYAsignaLoQueNoTeniaEmpresa() throws Exception {
        // una base de la version anterior: empresa con el nombre viejo, establecimiento sin empresa,
        // el administrador de antes (veia todo) y un usuario de carga con establecimiento asignado
        companies.save(new CompanyEntity(1L, "Ganadera de prueba", "2026-09"));
        EstablishmentEntity p = establishments.save(new EstablishmentEntity(null, "P", "Principal"));
        users.save(new UserEntity("admin", "hash", UserEntity.Role.ADMIN, null));
        UserEntity carga = new UserEntity("carga", "hash", UserEntity.Role.CARGA, null);
        carga.getEstablishments().add(p);
        users.save(carga);

        migracion.run(new DefaultApplicationArguments());

        assertEquals("Prueba", companies.findById(1L).orElseThrow().getName());
        assertEquals(1L, establishments.findById("P").orElseThrow().getCompanyId());
        UserEntity admin = users.findByUsername("admin").orElseThrow();
        assertEquals(UserEntity.Role.SUPERADMIN, admin.getRole());
        assertEquals(null, admin.getCompanyId());
        assertEquals(1L, users.findByUsername("carga").orElseThrow().getCompanyId());

        // es idempotente: una segunda pasada no cambia nada
        migracion.run(new DefaultApplicationArguments());
        assertEquals(UserEntity.Role.SUPERADMIN, users.findByUsername("admin").orElseThrow().getRole());
        assertNotNull(companies.findById(1L).orElseThrow());
    }
}
