@import com.kinetix.cattlestock.domain.Column
@import com.kinetix.cattlestock.domain.Species
@param com.kinetix.cattlestock.domain.Planilla planilla
@param java.util.List<com.kinetix.cattlestock.data.EstablishmentEntity> establecimientos
@param String establecimiento
@param String mes
@param String error
@param String usuario
@param boolean esAdmin
@param org.springframework.security.web.csrf.CsrfToken _csrf
<!DOCTYPE html>
<html lang="es">
    <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>Planilla mensual - CattleStock</title>
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
            <h1>Planilla mensual</h1>
            <form method="get" action="/planilla" class="filtros">
                <label>Establecimiento
                    <select name="establecimiento">
                        @for (var est : establecimientos)
                            <option value="${est.getCode()}" selected="${est.getCode().equals(establecimiento)}">${est.getName()}</option>
                        @endfor
                    </select>
                </label>
                <label>Mes <input type="month" name="mes" value="${mes}"></label>
                <button type="submit" class="boton boton-linea">Ver</button>
            </form>
            @if (error != null)
                <p class="error"><strong>${error}</strong></p>
            @endif
            @if (planilla != null)
                <h2>${planilla.establishment().name()} - ${planilla.month().toString()}</h2>
                @if (planilla.closed())
                    <p class="cerrada"><strong>Planilla cerrada.</strong> No se pueden cargar movimientos de este mes. Pedile a un administrador que la reabra si falta algo.</p>
                @endif
                <div class="acciones">
                    @if (planilla.closed())
                        <form method="post" action="/planilla/reabrir">
                            <input type="hidden" name="establecimiento" value="${establecimiento}">
                            <input type="hidden" name="mes" value="${mes}">
                            @if (_csrf != null)
                                <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                            @endif
                            <button type="submit" class="boton boton-linea">Reabrir mes</button>
                        </form>
                    @else
                        <form method="post" action="/planilla/cerrar" onsubmit="return confirm('Cerrar la planilla de ${mes}? Despues de cerrarla no se pueden cargar movimientos de ese mes y solo un administrador puede reabrirla.')">
                            <input type="hidden" name="establecimiento" value="${establecimiento}">
                            <input type="hidden" name="mes" value="${mes}">
                            @if (_csrf != null)
                                <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                            @endif
                            <button type="submit" class="boton boton-peligro">Cerrar mes</button>
                        </form>
                    @endif
                    <a class="boton boton-linea" href="/planilla/excel?establecimiento=${establecimiento}&mes=${mes}">Descargar Excel</a>
                </div>
                @for (var especie : Species.values())
                    @if (planilla.rows().stream().anyMatch(r -> r.category().species() == especie))
                        <h3>${especie == Species.VACUNOS ? "Vacunos" : especie == Species.YEGUARIZOS ? "Yeguarizos" : "Lanares"}</h3>
                        <div class="tabla-scroll">
                            <table class="planilla">
                                <thead>
                                    <tr>
                                        <th>Categoria</th>
                                        <th>Anterior</th>
                                        <th>Compra</th>
                                        <th>Nacimiento</th>
                                        <th>Traslado entra</th>
                                        <th>Cambio entra</th>
                                        <th>Venta</th>
                                        <th>Mortandad</th>
                                        <th>Consumo</th>
                                        <th>Traslado sale</th>
                                        <th>Cambio sale</th>
                                        <th>Entradas</th>
                                        <th>Salidas</th>
                                        <th>Final</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    @for (var fila : planilla.rows())
                                        @if (fila.category().species() == especie)
                                            <tr>
                                                <td>${fila.category().name()}</td>
                                                <td>${fila.anterior()}</td>
                                                <td>${fila.columns().get(Column.COMPRA)}</td>
                                                <td>${fila.columns().get(Column.NACIMIENTO)}</td>
                                                <td>${fila.columns().get(Column.TRASLADO_ENTRA)}</td>
                                                <td>${fila.columns().get(Column.CAMBIO_ENTRA)}</td>
                                                <td>${fila.columns().get(Column.VENTA)}</td>
                                                <td>${fila.columns().get(Column.MORTANDAD)}</td>
                                                <td>${fila.columns().get(Column.CONSUMO)}</td>
                                                <td>${fila.columns().get(Column.TRASLADO_SALE)}</td>
                                                <td>${fila.columns().get(Column.CAMBIO_SALE)}</td>
                                                <td>${fila.entradas()}</td>
                                                <td>${fila.salidas()}</td>
                                                <td><strong>${fila.finalStock()}</strong></td>
                                            </tr>
                                        @endif
                                    @endfor
                                    <tr>
                                        <td><strong>Total</strong></td>
                                        <td><strong>${planilla.totalFor(especie).anterior()}</strong></td>
                                        <td>${planilla.totalFor(especie).columns().get(Column.COMPRA)}</td>
                                        <td>${planilla.totalFor(especie).columns().get(Column.NACIMIENTO)}</td>
                                        <td>${planilla.totalFor(especie).columns().get(Column.TRASLADO_ENTRA)}</td>
                                        <td>${planilla.totalFor(especie).columns().get(Column.CAMBIO_ENTRA)}</td>
                                        <td>${planilla.totalFor(especie).columns().get(Column.VENTA)}</td>
                                        <td>${planilla.totalFor(especie).columns().get(Column.MORTANDAD)}</td>
                                        <td>${planilla.totalFor(especie).columns().get(Column.CONSUMO)}</td>
                                        <td>${planilla.totalFor(especie).columns().get(Column.TRASLADO_SALE)}</td>
                                        <td>${planilla.totalFor(especie).columns().get(Column.CAMBIO_SALE)}</td>
                                        <td></td>
                                        <td></td>
                                        <td><strong>${planilla.totalFor(especie).finalStock()}</strong></td>
                                    </tr>
                                </tbody>
                            </table>
                        </div>
                    @endif
                @endfor
                <p class="aviso"><strong>Total general: anterior ${planilla.totalGeneral().anterior()}, final ${planilla.totalGeneral().finalStock()}</strong></p>
            @endif
        </main>
    </body>
</html>
