@param String error
@param String hoy
@param String periodo
@param String fechaPorDefecto
@param String establecimiento
@param String especie
@param String fecha
@param String nota
@param java.util.Map<String, String> valores
@param java.util.List<com.kinetix.cattlestock.data.EstablishmentEntity> establecimientos
@param java.util.List<com.kinetix.cattlestock.data.CategoryEntity> categorias
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
        <title>Carga rápida - CattleStock</title>
        <link rel="stylesheet" href="/app.css">
        <meta name="htmx-config" content='{"globalViewTransitions":true}'>
        <script src="/htmx.min.js"></script>
    </head>
    <body>
        @template.fragment.barra(usuario = usuario, esAdmin = esAdmin, esSuperadmin = esSuperadmin, empresaNombre = empresaNombre, empresasMenu = empresasMenu, empresaActivaId = empresaActivaId, ruta = ruta, _csrf = _csrf)
        <main class="contenedor">
            <h1>Carga rápida</h1>
            @if (error != null)
                <p class="error"><strong>${error}</strong></p>
            @endif
            <p class="introduccion">Una fila por categoría, una fecha para todo el lote. Se cargan solo las celdas con número. Traslados y cambios de categoría van por el formulario de alta, porque necesitan destino.</p>
            <form method="post" action="/grilla">
                @if (_csrf != null)
                    <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                @endif
                <div class="lote-cabecera">
                    @template.fragment.periodo(nombre = "periodo", valor = periodo, actualizaFecha = true)
                    <label>Fecha
                        @template.fragment.campoFecha(fecha = fecha.isEmpty() ? fechaPorDefecto : fecha)
                    </label>
                    <label>Establecimiento
                        <select name="establecimiento" required
                                hx-get="/grilla/filas" hx-target="#filas" hx-include="closest form">
                            <option value=""></option>
                            @for (var est : establecimientos)
                                <option value="${est.getCode()}" selected="${est.getCode().equals(establecimiento)}">${est.getName()}</option>
                            @endfor
                        </select>
                    </label>
                    <label title="Las filas de otras especies quedan ocultas, no se pierden: lo que escribas en ellas se guarda igual.">Especie
                        <select name="especie"
                                hx-get="/grilla/filas" hx-target="#filas" hx-include="closest form" hx-trigger="change">
                            <option value="">Todas</option>
                            <option value="VACUNOS" selected="${"VACUNOS".equals(especie)}">Vacunos</option>
                            <option value="YEGUARIZOS" selected="${"YEGUARIZOS".equals(especie)}">Yeguarizos</option>
                            <option value="LANARES" selected="${"LANARES".equals(especie)}">Lanares</option>
                        </select>
                    </label>
                </div>
                @if (!especie.isEmpty())
                    <p class="ayuda">Las filas de otras especies quedan ocultas, no se pierden: si escribiste algo en ellas, se guarda igual.</p>
                @endif
                <div class="tabla-scroll">
                    <table class="grilla">
                        <thead>
                            <tr>
                                <th>Categoría</th>
                                <th>Compra</th>
                                <th>Nacimiento</th>
                                <th>Venta</th>
                                <th>Mortandad</th>
                                <th>Cueros</th>
                                <th>Consumo</th>
                                <th>Pastoreo</th>
                                <th title="Cantidad contada. El sistema calcula la diferencia contra la existencia y la asienta sola.">Recuento</th>
                            </tr>
                        </thead>
                        <tbody id="filas">
                            @template.fragment.filasGrilla(categorias = categorias, valores = valores, especie = especie)
                        </tbody>
                    </table>
                </div>
                <p>
                    <label>Nota (para todo el lote)<br>
                        <input type="text" name="nota" value="${nota}">
                    </label>
                </p>
                <p><button type="submit" class="boton">Guardar lote</button></p>
            </form>
        </main>
    </body>
</html>
