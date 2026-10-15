@param String appName
@param String mesActual
@param boolean esAdmin
@param java.util.List<com.kinetix.cattlestock.data.EstablishmentEntity> establecimientos
@param org.springframework.security.web.csrf.CsrfToken _csrf
<!DOCTYPE html>
<html lang="es">
    <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>${appName}</title>
        <link rel="stylesheet" href="/app.css">
        <script src="/htmx.min.js"></script>
    </head>
    <body>
        <header class="barra">
            <a class="marca" href="/"><span class="caravana" aria-hidden="true"></span>CattleStock</a>
            <nav>
                <a href="/alta">Cargar movimiento</a>
                <a href="/grilla">Carga rapida</a>
                <a href="/existencia">Existencia</a>
            </nav>
            <form method="post" action="/logout" class="salir">
                @if (_csrf != null)
                    <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                @endif
                <button type="submit">Salir</button>
            </form>
        </header>
        <main class="contenedor">
            <h1>${appName}</h1>
            <p class="introduccion">Registro de movimientos de hacienda.</p>
            @if (esAdmin)
                <p><a href="/configuracion">Configuracion</a></p>
            @endif
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
