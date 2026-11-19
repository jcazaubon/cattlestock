@param String error
@param String establecimiento
@param String especie
@param String categoria
@param java.util.Map<com.kinetix.cattlestock.domain.Species, java.util.List<com.kinetix.cattlestock.data.CategoryEntity>> grupos
@param String fecha
@param java.util.List<com.kinetix.cattlestock.data.EstablishmentEntity> establecimientos
@param boolean conExistencia
@param java.util.List<com.kinetix.cattlestock.web.ExistenciaController.Bloque> bloques
@param java.util.List<com.kinetix.cattlestock.web.ExistenciaController.Parte> porEstablecimiento
@param java.util.List<com.kinetix.cattlestock.web.ExistenciaController.Parte> porEspecie
@param com.kinetix.cattlestock.web.ExistenciaController.Total totalGeneral
@param boolean hayMovimiento
@param java.util.List<com.kinetix.cattlestock.web.ExistenciaController.Col> entradasVisibles
@param java.util.List<com.kinetix.cattlestock.web.ExistenciaController.Col> salidasVisibles
@param boolean trasladosInternos
@param String fechaTexto
@param String inicioMesTexto
@param int encontradas
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
        <title>Existencia - CattleStock</title>
        <link rel="stylesheet" href="/app.css">
        <meta name="htmx-config" content='{"globalViewTransitions":true}'>
        <script src="/htmx.min.js"></script>
    </head>
    <body>
        @template.fragment.barra(usuario = usuario, esAdmin = esAdmin, esSuperadmin = esSuperadmin, empresaNombre = empresaNombre, empresasMenu = empresasMenu, empresaActivaId = empresaActivaId, ruta = ruta, _csrf = _csrf)
        <main class="contenedor">
            <h1>Existencia</h1>
            <form method="get" action="/existencia" class="filtros">
                <label>Establecimiento
                    <select name="establecimiento"
                            hx-get="/existencia/categorias" hx-target="#categoria" hx-include="closest form" hx-trigger="change">
                        <option value="">Todos</option>
                        @for (var est : establecimientos)
                            <option value="${est.getCode()}" selected="${est.getCode().equals(establecimiento)}">${est.getName()}</option>
                        @endfor
                    </select>
                </label>
                <label>Especie
                    <select name="especie"
                            hx-get="/existencia/categorias" hx-target="#categoria" hx-include="closest form" hx-trigger="change">
                        <option value="">Todas</option>
                        <option value="VACUNOS" selected="${"VACUNOS".equals(especie)}">Vacunos</option>
                        <option value="YEGUARIZOS" selected="${"YEGUARIZOS".equals(especie)}">Yeguarizos</option>
                        <option value="LANARES" selected="${"LANARES".equals(especie)}">Lanares</option>
                    </select>
                </label>
                <label>Categoría
                    <select name="categoria" id="categoria">
                        @template.fragment.opcionesCategoria(grupos = grupos, categoria = categoria)
                    </select>
                </label>
                <label>Fecha <input type="date" name="fecha" value="${fecha}"></label>
                <label class="opcion-inline"><input type="checkbox" name="conExistencia" value="1" checked="${conExistencia}"> Ocultar categorías sin animales</label>
                <button type="submit" class="boton boton-linea">Ver</button>
            </form>
            @if (error != null)
                <p class="error"><strong>${error}</strong></p>
            @endif
            @if (encontradas == 0 && error == null)
                <p class="vacio">No hay categorías con esos filtros.</p>
            @endif
            @if (encontradas > 0)
                <section class="resumen-existencia">
                    <p class="total-linea"><strong>${totalGeneral.existencia()}</strong> animales al ${fechaTexto}</p>
                    <div class="repartos">
                    @if (!porEstablecimiento.isEmpty())
                        <div class="reparto">
                            <h2>Por establecimiento</h2>
                            @for (var p : porEstablecimiento)
                                <div class="parte">
                                    <span class="nombre">${p.nombre()}</span>
                                    <span class="medida"><span class="relleno" style="width: ${p.ancho()}%"></span></span>
                                    <span class="cantidad">${p.cantidad()}</span>
                                    <span class="pct">${p.pctTexto()}</span>
                                </div>
                            @endfor
                        </div>
                    @endif
                    @if (!porEspecie.isEmpty())
                        <div class="reparto">
                            <h2>Por especie</h2>
                            @for (var p : porEspecie)
                                <div class="parte">
                                    <span class="nombre">${p.nombre()}</span>
                                    <span class="medida"><span class="relleno" style="width: ${p.ancho()}%"></span></span>
                                    <span class="cantidad">${p.cantidad()}</span>
                                    <span class="pct">${p.pctTexto()}</span>
                                </div>
                            @endfor
                        </div>
                    @endif
                    </div>
                </section>
                @if (hayMovimiento)
                    <p class="periodo">Movimientos del mes hasta el ${fechaTexto}. El inicio es la existencia al ${inicioMesTexto}.</p>
                @endif
                @if (trasladosInternos)
                    <p class="periodo">Los traslados entre establecimientos figuran como entrada y como salida: en el total se compensan.</p>
                @endif
                @for (var bloque : bloques)
                    @if (bloques.size() > 1)
                        <h2 class="bloque-titulo">${bloque.establecimiento()}</h2>
                    @endif
                    <div class="tabla-scroll${hayMovimiento ? "" : " angosta"}">
                        <table class="existencia">
                            <thead>
                                <tr>
                                    <th rowspan="${hayMovimiento ? 2 : 1}">Categoría</th>
                                    @if (hayMovimiento)
                                        <th rowspan="2">Al ${inicioMesTexto}</th>
                                        @if (!entradasVisibles.isEmpty())
                                            <th class="grupo-entra" colspan="${entradasVisibles.size()}">Entradas</th>
                                        @endif
                                        @if (!salidasVisibles.isEmpty())
                                            <th class="grupo-sale" colspan="${salidasVisibles.size()}">Salidas</th>
                                        @endif
                                    @endif
                                    <th class="col-final" rowspan="${hayMovimiento ? 2 : 1}">Existencia</th>
                                    <th rowspan="${hayMovimiento ? 2 : 1}">% del total</th>
                                </tr>
                                @if (hayMovimiento)
                                    <tr class="subencabezado">
                                        @for (var c : entradasVisibles)
                                            <th class="col-entra">${c.titulo()}</th>
                                        @endfor
                                        @for (var c : salidasVisibles)
                                            <th class="col-sale">${c.titulo()}</th>
                                        @endfor
                                    </tr>
                                @endif
                            </thead>
                            <tbody>
                                @for (var seccion : bloque.secciones())
                                    @if (bloque.secciones().size() > 1)
                                        <tr class="seccion"><th colspan="${3 + (hayMovimiento ? 1 + entradasVisibles.size() + salidasVisibles.size() : 0)}">${seccion.especie()}</th></tr>
                                    @endif
                                    @for (var f : seccion.filas())
                                        <tr>
                                            <td>${f.categoria()}</td>
                                            @if (hayMovimiento)
                                                <td>${f.inicio()}</td>
                                                @for (var c : entradasVisibles)
                                                    <td class="col-entra">${f.texto(c.columna())}</td>
                                                @endfor
                                                @for (var c : salidasVisibles)
                                                    <td class="col-sale">${f.texto(c.columna())}</td>
                                                @endfor
                                            @endif
                                            <td class="col-final">${f.existencia()}</td>
                                            <td class="pct">${f.pctTexto()}</td>
                                        </tr>
                                    @endfor
                                    @if (seccion.filas().size() > 1)
                                        <tr class="subtotal">
                                            <td>Total ${seccion.especie().toLowerCase()}</td>
                                            @if (hayMovimiento)
                                                <td>${seccion.total().inicio()}</td>
                                                @for (var c : entradasVisibles)
                                                    <td class="col-entra">${seccion.total().texto(c.columna())}</td>
                                                @endfor
                                                @for (var c : salidasVisibles)
                                                    <td class="col-sale">${seccion.total().texto(c.columna())}</td>
                                                @endfor
                                            @endif
                                            <td class="col-final">${seccion.total().existencia()}</td>
                                            <td></td>
                                        </tr>
                                    @endif
                                @endfor
                                @if (bloque.secciones().size() > 1)
                                    <tr class="total-bloque">
                                        <td>${bloques.size() > 1 ? "Total " + bloque.establecimiento() : "Total general"}</td>
                                        @if (hayMovimiento)
                                            <td>${bloque.total().inicio()}</td>
                                            @for (var c : entradasVisibles)
                                                <td class="col-entra">${bloque.total().texto(c.columna())}</td>
                                            @endfor
                                            @for (var c : salidasVisibles)
                                                <td class="col-sale">${bloque.total().texto(c.columna())}</td>
                                            @endfor
                                        @endif
                                        <td class="col-final">${bloque.total().existencia()}</td>
                                        <td></td>
                                    </tr>
                                @endif
                            </tbody>
                        </table>
                    </div>
                @endfor
            @endif
        </main>
    </body>
</html>
