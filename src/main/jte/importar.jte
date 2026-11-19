@param java.util.List<com.kinetix.cattlestock.web.ConfiguracionController.FilaImportada> filas
@param String origen
@param long nuevas
@param String usuario
@param boolean esAdmin
@param boolean esSuperadmin
@param String empresaNombre
@param java.util.List<com.kinetix.cattlestock.data.CompanyEntity> empresasMenu
@param Long empresaActivaId
@param String ruta
@param org.springframework.security.web.csrf.CsrfToken _csrf
<!DOCTYPE html>
<html lang="es">
    <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>Importar categorías - CattleStock</title>
        <link rel="stylesheet" href="/app.css">
        <meta name="htmx-config" content='{"globalViewTransitions":true}'>
        <script src="/htmx.min.js"></script>
    </head>
    <body>
        @template.fragment.barra(usuario = usuario, esAdmin = esAdmin, esSuperadmin = esSuperadmin, empresaNombre = empresaNombre, empresasMenu = empresasMenu, empresaActivaId = empresaActivaId, ruta = ruta, _csrf = _csrf)
        <main class="contenedor">
            <p class="volver"><a href="/configuracion">&larr; Configuración</a></p>
            <h1>Importar categorías</h1>
            <p class="introduccion">Se detectaron ${filas.size()} categorías en ${origen}. Las que ya existen se saltean. Revisa los nombres y la especie, destilda las que no quieras y confirma.</p>
            <form method="post" action="/configuracion/categorias/importar/confirmar">
                @if (_csrf != null)
                    <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                @endif
                <div class="tabla-scroll angosta">
                    <table class="importar-tabla">
                        <thead>
                            <tr><th>Importar</th><th>Categoría</th><th>Especie</th></tr>
                        </thead>
                        <tbody>
                            @for (int i = 0; i < filas.size(); i++)
                                <tr class="${filas.get(i).existe() ? "ya-existe" : ""}">
                                    <td><input type="checkbox" name="incluir" value="${i}" checked="${!filas.get(i).existe()}" aria-label="Importar ${filas.get(i).nombre()}"></td>
                                    <td><input name="nombre" value="${filas.get(i).nombre()}" required>${filas.get(i).existe() ? " ya existe" : ""}</td>
                                    <td>
                                        <select name="especie">
                                            <option value="VACUNOS" selected="${filas.get(i).especie().equals("VACUNOS")}">Vacunos</option>
                                            <option value="YEGUARIZOS" selected="${filas.get(i).especie().equals("YEGUARIZOS")}">Yeguarizos</option>
                                            <option value="LANARES" selected="${filas.get(i).especie().equals("LANARES")}">Lanares</option>
                                        </select>
                                    </td>
                                </tr>
                            @endfor
                        </tbody>
                    </table>
                </div>
                <div class="acciones">
                    <button type="submit" class="boton">Importar las tildadas</button>
                    <a class="boton boton-linea" href="/configuracion">Cancelar</a>
                </div>
            </form>
        </main>
    </body>
</html>
