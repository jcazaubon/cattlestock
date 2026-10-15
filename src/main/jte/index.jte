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
        <script src="/htmx.min.js"></script>
    </head>
    <body>
        <h1>${appName}</h1>
        <p>Registro de movimientos de hacienda.</p>
        <p>
            <a href="/alta">Cargar movimiento</a> |
            <a href="/grilla">Carga rapida en grilla</a> |
            <a href="/existencia">Existencia</a>
            @if (esAdmin)
                | <a href="/configuracion">Configuracion</a>
            @endif
        </p>
        <h2>Planillas</h2>
        @if (establecimientos.isEmpty())
            <p>No hay establecimientos asignados. Pedile al administrador que te asigne.</p>
        @else
            <ul>
                @for (var est : establecimientos)
                    <li><a href="/planilla?establecimiento=${est.getCode()}&mes=${mesActual}">${est.getName()}</a></li>
                @endfor
            </ul>
        @endif
        <form method="post" action="/logout">
            @if (_csrf != null)
                <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
            @endif
            <button type="submit">Salir</button>
        </form>
    </body>
</html>
