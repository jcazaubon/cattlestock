@param String appName
@param String mesActual
@param boolean esAdmin
@param String usuario
@param java.util.List<com.kinetix.cattlestock.data.EstablishmentEntity> establecimientos
@param org.springframework.security.web.csrf.CsrfToken _csrf
<!DOCTYPE html>
<html lang="es">
    <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>${appName}</title>
        <link rel="stylesheet" href="/app.css">
        <meta name="htmx-config" content='{"globalViewTransitions":true}'>
        <script src="/htmx.min.js"></script>
    </head>
    <body>
        <header class="barra">
            <a class="marca" href="/"><span class="caravana" aria-hidden="true"></span>CattleStock</a>
            <nav hx-boost="true">
                <a href="/alta">Cargar movimiento</a>
                <a href="/grilla">Carga rapida</a>
                <a href="/existencia">Existencia</a>
            </nav>
            <div class="menu-usuario">
                <button type="button" class="usuario">
                    <span class="avatar" aria-hidden="true">${usuario.length() > 0 ? usuario.substring(0, 1).toUpperCase() : "C"}</span>
                    ${usuario}
                </button>
                <div class="menu-usuario-opciones">
                    @if (esAdmin)
                        <a href="/configuracion">Configuracion</a>
                        <div class="separador"></div>
                    @endif
                    <form method="post" action="/logout" hx-boost="false">
                        @if (_csrf != null)
                            <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                        @endif
                        <button type="submit">Salir</button>
                    </form>
                </div>
            </div>
        </header>
        <main class="contenedor">
            <h1>${appName}</h1>
            <p class="introduccion">Registro de movimientos de hacienda.</p>
            <h2>Planillas</h2>
            @if (establecimientos.isEmpty())
                <p class="vacio">No hay establecimientos asignados. Pedile al administrador que te asigne, o configure uno nuevo en Configuracion.</p>
            @else
                <ul class="lista-planillas">
                    @for (var est : establecimientos)
                        <li><a href="/planilla?establecimiento=${est.getCode()}&mes=${mesActual}">${est.getName()}</a></li>
                    @endfor
                </ul>
            @endif
        </main>
    </body>
</html>
