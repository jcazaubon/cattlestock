@param String appName
@param String mesActual
@param boolean esAdmin
@param boolean esSuperadmin
@param String empresaNombre
@param java.util.List<com.kinetix.cattlestock.data.CompanyEntity> empresasMenu
@param Long empresaActivaId
@param String ruta
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
        @template.fragment.barra(usuario = usuario, esAdmin = esAdmin, esSuperadmin = esSuperadmin, empresaNombre = empresaNombre, empresasMenu = empresasMenu, empresaActivaId = empresaActivaId, ruta = ruta, _csrf = _csrf)
        <main class="contenedor">
            <h1>Planillas</h1>
            <p class="subtitulo">Elegí un establecimiento para ver la planilla del mes.</p>
            @if (establecimientos.isEmpty())
                <p class="vacio">No hay establecimientos asignados. Pedile al administrador que te asigne uno. Si administras la empresa, agregalos en Configuración.</p>
            @else
                <ul class="lista-planillas">
                    @for (var est : establecimientos)
                        <li><a href="/planilla?establecimiento=${est.getCode()}&mes=${mesActual}"><span>${est.getName()}</span><span class="detalle">${mesActual}</span></a></li>
                    @endfor
                </ul>
            @endif
        </main>
    </body>
</html>
