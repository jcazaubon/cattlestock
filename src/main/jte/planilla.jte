@import com.kinetix.cattlestock.domain.Column
@import com.kinetix.cattlestock.domain.Species
@param com.kinetix.cattlestock.domain.Planilla planilla
@param java.util.List<com.kinetix.cattlestock.data.EstablishmentEntity> establecimientos
@param String establecimiento
@param String mes
@param String error
<!DOCTYPE html>
<html lang="es">
    <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>Planilla mensual - CattleStock</title>
        <script src="/htmx.min.js"></script>
    </head>
    <body>
        <h1>Planilla mensual</h1>
        <p><a href="/">Inicio</a> | <a href="/alta">Cargar movimiento</a></p>
        <form method="get" action="/planilla">
            <label>Establecimiento
                <select name="establecimiento">
                    @for (var est : establecimientos)
                        <option value="${est.getCode()}" selected="${est.getCode().equals(establecimiento)}">${est.getName()}</option>
                    @endfor
                </select>
            </label>
            <label>Mes <input type="month" name="mes" value="${mes}"></label>
            <button type="submit">Ver</button>
        </form>
        @if (error != null)
            <p class="error"><strong>${error}</strong></p>
        @endif
        @if (planilla != null)
            <h2>${planilla.establishment().name()} - ${planilla.month().toString()}</h2>
            @if (planilla.closed())
                <p><strong>Planilla cerrada.</strong></p>
                <form method="post" action="/planilla/reabrir">
                    <input type="hidden" name="establecimiento" value="${establecimiento}">
                    <input type="hidden" name="mes" value="${mes}">
                    <button type="submit">Reabrir mes</button>
                </form>
            @else
                <form method="post" action="/planilla/cerrar">
                    <input type="hidden" name="establecimiento" value="${establecimiento}">
                    <input type="hidden" name="mes" value="${mes}">
                    <button type="submit">Cerrar mes</button>
                </form>
            @endif
            <p><a href="/planilla/excel?establecimiento=${establecimiento}&mes=${mes}">Descargar Excel</a></p>
            @for (var especie : Species.values())
                @if (planilla.rows().stream().anyMatch(r -> r.category().species() == especie))
                    <h3>${especie == Species.VACUNOS ? "Vacunos" : especie == Species.YEGUARIZOS ? "Yeguarizos" : "Lanares"}</h3>
                    <table border="1">
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
                @endif
            @endfor
            <p><strong>Total general: anterior ${planilla.totalGeneral().anterior()}, final ${planilla.totalGeneral().finalStock()}</strong></p>
        @endif
    </body>
</html>
