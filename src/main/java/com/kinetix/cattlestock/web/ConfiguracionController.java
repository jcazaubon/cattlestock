package com.kinetix.cattlestock.web;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.kinetix.cattlestock.app.ImportadorCategorias;
import com.kinetix.cattlestock.app.LedgerService;
import com.kinetix.cattlestock.app.StockInicialExcel;
import com.kinetix.cattlestock.app.UserService;
import com.kinetix.cattlestock.data.CategoryEntity;
import com.kinetix.cattlestock.data.OpeningEntity;
import com.kinetix.cattlestock.data.OpeningRepository;
import com.kinetix.cattlestock.data.UserEntity;
import com.kinetix.cattlestock.data.UserRepository;
import com.kinetix.cattlestock.domain.DomainException;
import com.kinetix.cattlestock.domain.Species;

/**
 * Configuracion de la empresa: establecimientos, categorias, stock inicial y usuarios. La empresa es la del
 * administrador (o la que el superadmin eligio): nunca se toca un establecimiento ni un usuario de otra.
 * Las empresas nuevas las da de alta el superadmin en /admin.
 */
@Controller
public class ConfiguracionController {

    /** Una categoria con su stock inicial ya cargado, para la tabla de configuracion. */
    public record FilaCategoria(CategoryEntity categoria, int cantidad) {
    }

    /** Un valor de stock inicial leido del Excel, con lo que hay hoy, para la vista previa. */
    public record FilaStock(String establecimiento, String categoria, Integer actual, int nuevo, boolean cambia,
                            String valor) {
    }

    /** Una categoria propuesta en la vista previa de la importacion. */
    public record FilaImportada(String nombre, String especie, boolean existe) {
    }

    private final LedgerService service;
    private final UserService userService;
    private final ImportadorCategorias importador;
    private final StockInicialExcel stockExcel;
    private final OpeningRepository openings;
    private final UserRepository users;
    private final CurrentUser currentUser;

    public ConfiguracionController(LedgerService service, UserService userService, ImportadorCategorias importador,
                                    StockInicialExcel stockExcel, OpeningRepository openings, UserRepository users, CurrentUser currentUser) {
        this.service = service;
        this.userService = userService;
        this.importador = importador;
        this.stockExcel = stockExcel;
        this.openings = openings;
        this.users = users;
        this.currentUser = currentUser;
    }

    @GetMapping("/configuracion")
    public String configuracion(Model model,
                                 @RequestParam(required = false) String establecimiento,
                                 @RequestParam(required = false) String error,
                                 @RequestParam(required = false) String aviso,
                                 @RequestParam(required = false) String seccion,
                                 @RequestParam(required = false) String editar) {
        String seleccionado = currentUser.perteneceALaEmpresa(s(establecimiento)) ? s(establecimiento) : "";
        Long empresa = currentUser.empresaId().orElse(null);
        model.addAttribute("error", error);
        model.addAttribute("aviso", aviso);
        model.addAttribute("editar", s(editar));
        // un establecimiento elegido (editar y stock) siempre cae en su pestana
        model.addAttribute("seccion", seleccionado.isEmpty() && ("categorias".equals(seccion) || "stock".equals(seccion) || "usuarios".equals(seccion))
                ? seccion : "establecimientos");
        model.addAttribute("empresa", currentUser.empresa().orElse(null));
        model.addAttribute("establecimientos", currentUser.establecimientosDeLaEmpresa());
        model.addAttribute("seleccionado", seleccionado);
        model.addAttribute("seleccionadoNombre", currentUser.establecimientosDeLaEmpresa().stream()
                .filter(e -> e.getCode().equals(seleccionado)).findFirst().map(e -> e.getName()).orElse(""));
        model.addAttribute("filas", filasDe(seleccionado));
        model.addAttribute("categoriasEmpresa", listadoDeCategorias());
        model.addAttribute("usuarios", empresa == null ? List.<UserEntity>of()
                : users.findByCompanyIdOrderByUsernameAsc(empresa));
        return "configuracion";
    }

    @PostMapping("/configuracion/establecimientos")
    public String crearEstablecimiento(@RequestParam(required = false) String codigo, @RequestParam String nombre) {
        String code = s(codigo).isBlank() ? null : codigo.trim();
        try {
            String n = texto(nombre, "Escribi el nombre");
            code = code == null ? service.codigoDisponible(n) : code;
            service.createEstablishment(empresa(), code, n);
        } catch (DomainException | IllegalArgumentException e) {
            return conError(e.getMessage());
        }
        return "redirect:/configuracion?establecimiento=" + code;
    }

    /** Cambia el nombre de un establecimiento de la empresa. El codigo queda como estaba. */
    @PostMapping("/configuracion/establecimientos/{codigo}")
    public String editarEstablecimiento(@PathVariable String codigo, @RequestParam String nombre) {
        try {
            propio(codigo);
            service.renameEstablishment(codigo, texto(nombre, "Escribi el nombre"));
        } catch (DomainException | IllegalArgumentException e) {
            return "redirect:/configuracion?establecimiento=" + codigo
                    + "&error=" + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
        }
        return "redirect:/configuracion?establecimiento=" + codigo;
    }

    /** Agrega una categoria al catalogo de la empresa: la pueden usar todos sus establecimientos. */
    @PostMapping("/configuracion/categorias")
    public String crearCategoria(@RequestParam(required = false) String codigo,
                                 @RequestParam String nombre, @RequestParam String especie,
                                 @RequestParam(required = false) String establecimiento) {
        try {
            Species sp = especieDe(especie);
            String n = texto(nombre, "Escribi el nombre");
            String code = s(codigo).isBlank() ? service.codigoCategoriaDisponible(empresa(), n) : codigo.trim();
            service.createCategory(empresa(), code, n, sp);
        } catch (DomainException | IllegalArgumentException e) {
            return conError("categorias", e.getMessage());
        }
        return "redirect:/configuracion?seccion=categorias";
    }

    /** Cambia el nombre de una categoria (en la misma fila de la lista). */
    @PostMapping("/configuracion/categorias/{codigo}/nombre")
    public String renombrarCategoria(@PathVariable String codigo, @RequestParam String nombre) {
        try {
            service.renameCategory(empresa(), codigo, texto(nombre, "Escribi el nombre"));
        } catch (DomainException | IllegalArgumentException e) {
            return "redirect:/configuracion?seccion=categorias&editar=" + URLEncoder.encode(codigo, StandardCharsets.UTF_8)
                    + "&error=" + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
        }
        return "redirect:/configuracion?seccion=categorias";
    }

    /** Sube una categoria en el orden de la planilla. */
    @PostMapping("/configuracion/categorias/{codigo}/subir")
    public String subirCategoria(@PathVariable String codigo,
                                 @org.springframework.web.bind.annotation.RequestHeader(value = "HX-Request", required = false) String htmx,
                                 Model model) {
        return moverCategoria(codigo, -1, htmx != null, model);
    }

    /** Baja una categoria en el orden de la planilla. */
    @PostMapping("/configuracion/categorias/{codigo}/bajar")
    public String bajarCategoria(@PathVariable String codigo,
                                 @org.springframework.web.bind.annotation.RequestHeader(value = "HX-Request", required = false) String htmx,
                                 Model model) {
        return moverCategoria(codigo, 1, htmx != null, model);
    }

    /**
     * Mueve una categoria. Con htmx devuelve solo la tabla ya reordenada (sin recargar la pagina) y marca la fila
     * movida; sin htmx, por ejemplo con JavaScript apagado, vuelve a la pagina como cualquier formulario.
     */
    private String moverCategoria(String codigo, int delta, boolean htmx, Model model) {
        String error = null;
        try {
            service.moveCategory(empresa(), codigo, delta);
        } catch (DomainException e) {
            error = e.getMessage();
        }
        if (htmx) {
            model.addAttribute("categoriasEmpresa", listadoDeCategorias());
            model.addAttribute("editar", "");
            model.addAttribute("movida", error == null ? codigo : "");
            model.addAttribute("error", error);
            return "fragment/tablaCategorias";
        }
        return error == null ? "redirect:/configuracion?seccion=categorias" : conError("categorias", error);
    }

    @PostMapping("/configuracion/categorias/{codigo}/estado")
    public String estadoCategoria(@PathVariable String codigo, @RequestParam boolean habilitada) {
        try {
            service.setCategoryEnabled(empresa(), codigo, habilitada);
        } catch (DomainException e) {
            return conError("categorias", e.getMessage());
        }
        return "redirect:/configuracion?seccion=categorias";
    }

    @PostMapping("/configuracion/categorias/{codigo}/baja")
    public String bajaCategoria(@PathVariable String codigo) {
        try {
            service.deleteCategory(empresa(), codigo);
        } catch (DomainException e) {
            return conError("categorias", e.getMessage());
        }
        return "redirect:/configuracion?seccion=categorias&aviso="
                + URLEncoder.encode("Categoria dada de baja.", StandardCharsets.UTF_8);
    }

    /** Lee las categorias de un Excel, un archivo de texto o un texto pegado y las muestra para revisar. */
    @PostMapping("/configuracion/categorias/importar")
    public String importar(@RequestParam(required = false) MultipartFile archivo,
                           @RequestParam(required = false) String texto, Model model) {
        ImportadorCategorias.Lectura lectura;
        try {
            if (archivo != null && !archivo.isEmpty()) {
                lectura = importador.leer(archivo.getOriginalFilename(), archivo.getBytes());
            } else if (!s(texto).isBlank()) {
                lectura = new ImportadorCategorias.Lectura(importador.deTexto(texto), "el texto pegado");
            } else {
                return conError("categorias", "Elegí un archivo o pega las categorias, una por linea");
            }
        } catch (java.io.IOException e) {
            return conError("categorias", e.getMessage());
        }
        if (lectura.nombres().isEmpty()) {
            return conError("categorias", "No encontre categorias en " + lectura.origen()
                    + ". Proba pegandolas, una por linea.");
        }
        Long empresa = empresa();
        java.util.Set<String> existentes = service.categoriasDeEmpresa(empresa).stream()
                .map(c -> c.getName().trim().toLowerCase()).collect(Collectors.toSet());
        List<FilaImportada> filas = lectura.nombres().stream()
                .map(n -> new FilaImportada(n, ImportadorCategorias.adivinarEspecie(n).name(),
                        existentes.contains(n.trim().toLowerCase())))
                .toList();
        model.addAttribute("filas", filas);
        model.addAttribute("origen", lectura.origen());
        model.addAttribute("nuevas", filas.stream().filter(f -> !f.existe()).count());
        return "importar";
    }

    /** Crea las categorias que la persona dejo tildadas, todas o ninguna. */
    @PostMapping("/configuracion/categorias/importar/confirmar")
    public String confirmarImportacion(@RequestParam(required = false) List<String> nombre,
                                       @RequestParam(required = false) List<String> especie,
                                       @RequestParam(required = false) List<Integer> incluir) {
        try {
            List<LedgerService.NuevaCategoria> nuevas = new java.util.ArrayList<>();
            if (nombre != null && incluir != null) {
                for (int i : incluir) {
                    if (i < 0 || i >= nombre.size() || especie == null || i >= especie.size()) continue;
                    nuevas.add(new LedgerService.NuevaCategoria(nombre.get(i), especieDe(especie.get(i))));
                }
            }
            int creadas = service.importCategories(empresa(), nuevas);
            return "redirect:/configuracion?seccion=categorias&aviso=" + URLEncoder.encode(creadas == 1
                    ? "Se importo 1 categoria." : "Se importaron " + creadas + " categorias.", StandardCharsets.UTF_8);
        } catch (DomainException | IllegalArgumentException e) {
            return conError("categorias", e.getMessage());
        }
    }

    @PostMapping("/configuracion/stock")
    public String stock(@RequestParam String establecimiento, @RequestParam Map<String, String> valores) {
        try {
            propio(establecimiento);
            for (Map.Entry<String, String> entrada : valores.entrySet()) {
                String clave = entrada.getKey();
                if (!clave.startsWith("stock_") || entrada.getValue() == null || entrada.getValue().isBlank()) {
                    continue;
                }
                String categoria = clave.substring("stock_".length());
                service.setOpening(establecimiento, categoria, Integer.parseInt(entrada.getValue().trim()));
            }
        } catch (DomainException | IllegalArgumentException e) {
            return "redirect:/configuracion?establecimiento=" + establecimiento
                    + "&error=" + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
        }
        return "redirect:/configuracion?establecimiento=" + establecimiento;
    }

    /** La planilla de Excel para cargar el stock inicial de toda la empresa: ya trae las categorias y lo cargado. */
    @GetMapping("/configuracion/stock/plantilla")
    public void plantilla(jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        byte[] datos;
        String nombre;
        try {
            Long empresa = empresa();
            nombre = currentUser.empresa().map(c -> c.getName()).orElse("empresa");
            datos = stockExcel.plantilla(nombre, currentUser.establecimientosDeLaEmpresa(),
                    service.categoriasDeEmpresa(empresa), service.aperturasDeEmpresa(empresa));
        } catch (DomainException e) {
            response.sendRedirect(conError("stock", e.getMessage()).substring("redirect:".length()));
            return;
        }
        String archivo = java.text.Normalizer.normalize(nombre, java.text.Normalizer.Form.NFD)
                .replaceAll("[^A-Za-z0-9]+", "-").toLowerCase();
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=stock-inicial-" + archivo + ".xlsx");
        response.getOutputStream().write(datos);
    }

    /** Lee el Excel subido y muestra lo que cambiaria, sin aplicar nada. */
    @PostMapping("/configuracion/stock/importar")
    public String importarStock(@RequestParam(required = false) MultipartFile archivo, Model model) {
        if (archivo == null || archivo.isEmpty()) {
            return conError("stock", "Elegí el archivo de Excel con el stock");
        }
        StockInicialExcel.Lectura lectura;
        Map<String, Integer> actuales;
        try {
            Long empresa = empresa();
            lectura = stockExcel.leer(archivo.getBytes(), currentUser.establecimientosDeLaEmpresa(),
                    service.categoriasDeEmpresa(empresa));
            actuales = service.aperturasDeEmpresa(empresa);
        } catch (java.io.IOException | DomainException e) {
            return conError("stock", e.getMessage());
        }
        if (lectura.celdas().isEmpty()) {
            return conError("stock", "No encontre cantidades para importar"
                    + (lectura.problemas().isEmpty() ? "" : ": " + lectura.problemas().get(0)));
        }
        List<FilaStock> filas = lectura.celdas().stream().map(c -> {
            Integer actual = actuales.get(StockInicialExcel.clave(c.establecimientoCodigo(), c.categoriaCodigo()));
            return new FilaStock(c.establecimiento(), c.categoria(), actual, c.cantidad(),
                    actual == null || actual != c.cantidad(),
                    c.establecimientoCodigo() + "|" + c.categoriaCodigo() + "|" + c.cantidad());
        }).toList();
        model.addAttribute("filas", filas);
        model.addAttribute("problemas", lectura.problemas());
        model.addAttribute("cambian", filas.stream().filter(FilaStock::cambia).count());
        return "importar-stock";
    }

    /** Aplica el stock que se vio en la vista previa, todo o nada. */
    @PostMapping("/configuracion/stock/importar/confirmar")
    public String confirmarStock(@RequestParam(required = false) List<String> celda) {
        try {
            List<LedgerService.NuevaApertura> lista = new java.util.ArrayList<>();
            if (celda != null) {
                for (String c : celda) {
                    String[] p = c.split("[|]");
                    if (p.length != 3) continue;
                    lista.add(new LedgerService.NuevaApertura(p[0], p[1], Integer.parseInt(p[2])));
                }
            }
            int cambiados = service.importOpenings(empresa(), lista);
            return "redirect:/configuracion?seccion=stock&aviso=" + URLEncoder.encode(cambiados == 1
                    ? "Se actualizo 1 valor de stock inicial." : "Se actualizaron " + cambiados
                    + " valores de stock inicial.", StandardCharsets.UTF_8);
        } catch (DomainException | IllegalArgumentException e) {
            return conError("stock", e.getMessage());
        }
    }

    @PostMapping("/configuracion/usuarios")
    public String crearUsuario(@RequestParam String username,
                               @RequestParam String password,
                               @RequestParam String rol,
                               @RequestParam(required = false) List<String> establecimientos) {
        try {
            userService.crear(texto(username, "Escribi el usuario"), password, rolDe(rol), empresa(),
                    propios(establecimientos));
        } catch (DomainException | IllegalArgumentException e) {
            return conError("usuarios", e.getMessage());
        }
        return "redirect:/configuracion?seccion=usuarios";
    }

    /** Asigna rol y establecimientos a un usuario de la empresa. */
    @PostMapping("/configuracion/usuarios/{id}")
    public String actualizarUsuario(@PathVariable Long id, @RequestParam String rol,
                                    @RequestParam(required = false) List<String> establecimientos) {
        try {
            userService.actualizar(id, empresa(), rolDe(rol), propios(establecimientos));
        } catch (DomainException | IllegalArgumentException e) {
            return conError("usuarios", e.getMessage());
        }
        return "redirect:/configuracion?seccion=usuarios";
    }

    // --- Helpers ---

    private Long empresa() {
        return currentUser.empresaId().orElseThrow(() -> new DomainException("La empresa no esta configurada"));
    }

    /** Falla si el establecimiento no es de la empresa del usuario. */
    private void propio(String establecimiento) {
        if (!currentUser.perteneceALaEmpresa(establecimiento)) {
            throw new DomainException("El establecimiento " + establecimiento + " no es de tu empresa");
        }
    }

    private List<String> propios(List<String> establecimientos) {
        List<String> lista = establecimientos == null ? List.of() : establecimientos;
        lista.forEach(this::propio);
        return lista;
    }

    /** Un administrador de empresa asigna solo ADMIN o CARGA: el superadmin se da de alta en /admin. */
    private UserEntity.Role rolDe(String rol) {
        UserEntity.Role role;
        try {
            role = UserEntity.Role.valueOf(texto(rol, "Elegí el rol"));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("El rol no es valido");
        }
        if (role == UserEntity.Role.SUPERADMIN) throw new IllegalArgumentException("El rol no es valido");
        return role;
    }

    /** Las categorias de la empresa, en el orden de la planilla. */
    private List<CategoryEntity> listadoDeCategorias() {
        return currentUser.empresaId().map(service::categoriasDeEmpresa).orElse(List.of());
    }

    private Species especieDe(String especie) {
        try {
            return Species.valueOf(especie);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Elegí la especie");
        }
    }

    private List<FilaCategoria> filasDe(String establecimiento) {
        if (establecimiento.isBlank()) return List.of();
        Map<String, Integer> stock = openings.findByEstablishmentCodeIn(List.of(establecimiento)).stream()
                .collect(Collectors.toMap(OpeningEntity::getCategoryCode, OpeningEntity::getQuantity));
        return service.categoriasDe(establecimiento).stream()
                .map(c -> new FilaCategoria(c, stock.getOrDefault(c.getCode(), -1)))
                .toList();
    }

    /** Vuelve a Configuracion con el aviso de error, a la pestana donde se estaba (la primera si es null). */
    private String conError(String seccion, String mensaje) {
        return "redirect:/configuracion?" + (seccion == null ? "" : "seccion=" + seccion + "&")
                + "error=" + URLEncoder.encode(mensaje, StandardCharsets.UTF_8);
    }

    private String conError(String mensaje) {
        return conError(null, mensaje);
    }

    private String texto(String valor, String mensajeSiFalta) {
        if (valor == null || valor.isBlank()) throw new IllegalArgumentException(mensajeSiFalta);
        return valor.trim();
    }

    private static String s(String valor) {
        return valor == null ? "" : valor;
    }
}
