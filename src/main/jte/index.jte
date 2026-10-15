@param String appName
@param String mesActual
@param java.util.List<com.kinetix.cattlestock.data.EstablishmentEntity> establecimientos
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
        <p><a href="/alta">Cargar movimiento</a> | <a href="/grilla">Carga rapida en grilla</a> | <a href="/existencia">Existencia</a> | <a href="/configuracion">Configuracion</a></p>
        <h2>Planillas</h2>
        @if (establecimientos.isEmpty())
            <p>No hay establecimientos configurados todavia.</p>
        @else
            <ul>
                @for (var est : establecimientos)
                    <li><a href="/planilla?establecimiento=${est.getCode()}&mes=${mesActual}">${est.getName()}</a></li>
                @endfor
            </ul>
        @endif
    </body>
</html>
