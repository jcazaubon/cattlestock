@param String error
@param com.kinetix.cattlestock.web.AltaController.Disponible disponible
@param String hoy
@param String periodo
@param String fechaPorDefecto
@param String establecimiento
@param String fecha
@param String nota
@param String usuario
@param boolean esAdmin
@param boolean esSuperadmin
@param String empresaNombre
@param java.util.List<com.kinetix.cattlestock.data.CompanyEntity> empresasMenu
@param Long empresaActivaId
@param String ruta
@param org.springframework.security.web.csrf.CsrfToken _csrf
@param java.util.List<com.kinetix.cattlestock.data.EstablishmentEntity> establecimientos
@param java.util.List<com.kinetix.cattlestock.data.CategoryEntity> categorias
@param java.util.List<com.kinetix.cattlestock.web.AltaController.LineaForm> lineas
@param java.util.Map<java.lang.Integer, java.util.List<com.kinetix.cattlestock.data.CategoryEntity>> destinoCategoriasPorIndice
<!DOCTYPE html>
<html lang="es">
    <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>Cargar movimientos - CattleStock</title>
        <link rel="stylesheet" href="/app.css">
        <meta name="htmx-config" content='{"globalViewTransitions":true}'>
        <script src="/htmx.min.js"></script>
    </head>
    <body>
        @template.fragment.barra(usuario = usuario, esAdmin = esAdmin, esSuperadmin = esSuperadmin, empresaNombre = empresaNombre, empresasMenu = empresasMenu, empresaActivaId = empresaActivaId, ruta = ruta, _csrf = _csrf)
        <main class="contenedor">
            <h1>Cargar movimiento</h1>
            <p class="introduccion">Una linea por movimiento, un solo guardado: el lote entra completo o no entra nada. Traslados y cambios de categoría llevan su destino en la misma linea, por partida doble.</p>
            @if (error != null)
                <p class="error"><strong>${error}</strong></p>
            @endif
            <form method="post" action="/movimientos">
                @if (_csrf != null)
                    <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                @endif
                <div class="lote-cabecera"
                     hx-get="/alta/disponible" hx-trigger="change, fecha-actualizada from:body" hx-target="#disponible" hx-include="closest form" hx-swap="innerHTML">
                    <label>Período
                        <input type="month" name="periodo" value="${periodo}" aria-label="Período (mes y año)"
                               hx-get="/campo-fecha" hx-target="#fecha" hx-swap="outerHTML" hx-trigger="change" hx-include="this">
                    </label>
                    <label>Fecha
                        @template.fragment.campoFecha(fecha = fecha.isEmpty() ? fechaPorDefecto : fecha)
                    </label>
                    <label>Establecimiento
                        <select name="establecimiento" required
                                hx-get="/alta/lineas" hx-target="#lineas" hx-include="closest form">
                            <option value=""></option>
                            @for (var est : establecimientos)
                                <option value="${est.getCode()}" selected="${est.getCode().equals(establecimiento)}">${est.getName()}</option>
                            @endfor
                        </select>
                    </label>
                </div>
                <div id="disponible">
                    @template.fragment.disponible(disponible = disponible)
                </div>
                <div id="lineas">
                    @template.fragment.lineas(lineas = lineas, categorias = categorias,
                            establecimientos = establecimientos, establecimiento = establecimiento,
                            destinoCategoriasPorIndice = destinoCategoriasPorIndice)
                </div>
                <div class="agregar-linea">
                    <button type="button" class="boton boton-linea"
                            hx-get="/alta/linea" hx-target="#lineas" hx-swap="beforeend" hx-include="closest form">
                        Agregar otro movimiento
                    </button>
                    <span class="ayuda">Todos los movimientos del lote se guardan juntos: entran todos o no entra ninguno.</span>
                </div>
                @if (esAdmin)
                    <p class="ayuda">Si falta una categoría, agregalá en <a href="/configuracion${establecimiento.isEmpty() ? "" : "?establecimiento=" + establecimiento}">Configuración</a>.</p>
                @endif
                <div class="lote-pie">
                    <label class="nota-lote">Nota del lote
                        <input type="text" name="nota" value="${nota}">
                    </label>
                    <button type="submit" class="boton">Guardar lote</button>
                </div>
            </form>
        </main>
    </body>
</html>
