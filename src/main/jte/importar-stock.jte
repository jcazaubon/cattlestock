@param java.util.List<com.kinetix.cattlestock.web.ConfiguracionController.FilaStock> filas
@param java.util.List<String> problemas
@param long cambian
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
        <title>Importar stock inicial - CattleStock</title>
        <link rel="stylesheet" href="/app.css">
        <meta name="htmx-config" content='{"globalViewTransitions":true}'>
        <script src="/htmx.min.js"></script>
    </head>
    <body>
        @template.fragment.barra(usuario = usuario, esAdmin = esAdmin, esSuperadmin = esSuperadmin, empresaNombre = empresaNombre, empresasMenu = empresasMenu, empresaActivaId = empresaActivaId, ruta = ruta, _csrf = _csrf)
        <main class="contenedor config">
            <p class="volver"><a href="/configuracion?seccion=stock">&larr; Stock inicial</a></p>
            <h1>Importar stock inicial</h1>
            <p class="subtitulo">El archivo trae ${filas.size() == 1 ? "1 valor" : filas.size() + " valores"} y ${cambian == 1 ? "1 cambia" : cambian + " cambian"}. Nada se aplica hasta que confirmes.</p>
            @if (!problemas.isEmpty())
                <div class="advertencia">
                    <strong>Se ignoro ${problemas.size() == 1 ? "1 cosa" : problemas.size() + " cosas"} del archivo:</strong>
                    <ul>
                        @for (var p : problemas)
                            <li>${p}</li>
                        @endfor
                    </ul>
                </div>
            @endif
            <form method="post" action="/configuracion/stock/importar/confirmar">
                @if (_csrf != null)
                    <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                @endif
                @for (var f : filas)
                    <input type="hidden" name="celda" value="${f.valor()}">
                @endfor
                <div class="tabla-scroll">
                    <table class="stock-previa">
                        <thead>
                            <tr><th>Establecimiento</th><th>Categoría</th><th>Hoy</th><th>Nuevo</th></tr>
                        </thead>
                        <tbody>
                            @for (var f : filas)
                                <tr class="${f.cambia() ? "" : "sin-cambio"}">
                                    <td>${f.establecimiento()}</td>
                                    <td>${f.categoria()}</td>
                                    <td class="num">${f.actual() == null ? "-" : String.valueOf(f.actual())}</td>
                                    <td class="num"><strong>${f.nuevo()}</strong></td>
                                </tr>
                            @endfor
                        </tbody>
                    </table>
                </div>
                <div class="acciones">
                    <button type="submit" class="boton">Aplicar ${cambian == 1 ? "1 cambio" : cambian + " cambios"}</button>
                    <a class="boton boton-linea" href="/configuracion?seccion=stock">Cancelar</a>
                </div>
            </form>
        </main>
    </body>
</html>
