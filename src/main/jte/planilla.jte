@import com.kinetix.cattlestock.domain.Column
@import com.kinetix.cattlestock.domain.Species
@import java.time.format.TextStyle
@import java.util.Locale
@param com.kinetix.cattlestock.domain.Planilla planilla
@param java.util.List<com.kinetix.cattlestock.data.EstablishmentEntity> establecimientos
@param String establecimiento
@param String mes
@param java.util.List<String> meses
@param int mesNumero
@param java.util.List<Integer> anios
@param int anio
@param String error
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
        <title>Planilla mensual - CattleStock</title>
        <link rel="stylesheet" href="/app.css">
        <meta name="htmx-config" content='{"globalViewTransitions":true}'>
        <script src="/htmx.min.js"></script>
    </head>
    <body>
        @template.fragment.barra(usuario = usuario, esAdmin = esAdmin, esSuperadmin = esSuperadmin, empresaNombre = empresaNombre, empresasMenu = empresasMenu, empresaActivaId = empresaActivaId, ruta = ruta, _csrf = _csrf)
        <main class="contenedor">
            <div class="panel-planilla">
                <form method="get" action="/planilla" class="filtros">
                    <label>Establecimiento
                        <select name="establecimiento">
                            @for (var est : establecimientos)
                                <option value="${est.getCode()}" selected="${est.getCode().equals(establecimiento)}">${est.getName()}</option>
                            @endfor
                        </select>
                    </label>
                    <label>Mes
                        <select name="mesNumero">
                            @for (int i = 0; i < meses.size(); i++)
                                <option value="${i + 1}" selected="${i + 1 == mesNumero}">${meses.get(i)}</option>
                            @endfor
                        </select>
                    </label>
                    <label>Año
                        <select name="anio">
                            @for (var a : anios)
                                <option value="${a}" selected="${a == anio}">${a}</option>
                            @endfor
                        </select>
                    </label>
                    <button type="submit" class="boton boton-linea">Ver</button>
                </form>
                @if (planilla != null)
                    <div class="acciones">
                        <a class="boton boton-linea" href="/planilla/excel?establecimiento=${establecimiento}&mes=${mes}">Descargar Excel</a>
                        <a class="boton boton-linea" href="/planilla/pdf?establecimiento=${establecimiento}&mes=${mes}">Descargar PDF</a>
                        <span class="separador-accion" aria-hidden="true"></span>
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
                            <form method="post" action="/planilla/cerrar" data-confirmar="${planilla.establishment().name()}: despues de cerrarla no se pueden cargar movimientos de ese mes y solo un administrador puede reabrirla."
                                  data-confirmar-titulo="Cerrar la planilla de ${mes}" data-confirmar-boton="Cerrar mes">
                                <input type="hidden" name="establecimiento" value="${establecimiento}">
                                <input type="hidden" name="mes" value="${mes}">
                                @if (_csrf != null)
                                    <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                                @endif
                                <button type="submit" class="boton boton-peligro">Cerrar mes</button>
                            </form>
                        @endif
                    </div>
                @endif
            </div>
            @if (planilla != null)
                <h1 class="titulo-resultado">
                    ${planilla.establishment().name()} · ${planilla.month().getMonth().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es"))} ${planilla.month().getYear()}
                    @if (planilla.closed())
                        <span class="estado-cerrada">Cerrada</span>
                    @endif
                </h1>
            @else
                <h1 class="titulo-resultado">Planilla mensual</h1>
            @endif
            @if (error != null)
                <p class="error"><strong>${error}</strong></p>
            @endif
            @if (planilla != null && planilla.closed())
                <p class="cerrada"><strong>Planilla cerrada.</strong> No se pueden cargar movimientos de este mes. Pedile a un administrador que la reabra si falta algo.</p>
            @endif
            @if (planilla != null)
                @if (com.kinetix.cattlestock.web.PlanillaVista.filasSinMovimiento(planilla) > 0)
                    <div class="solo-movil ver-todas">
                        <input type="checkbox" id="ver-todas">
                        <label for="ver-todas">Mostrar las ${com.kinetix.cattlestock.web.PlanillaVista.filasSinMovimiento(planilla)} categorías sin movimiento y las columnas vacías</label>
                    </div>
                @endif
                @for (var especie : Species.values())
                    @if (planilla.rows().stream().anyMatch(r -> r.category().species() == especie))
                        !{var vacias = com.kinetix.cattlestock.web.PlanillaVista.columnasVacias(planilla, especie);}
                        <h2 class="especie">${especie == Species.VACUNOS ? "Vacunos" : especie == Species.YEGUARIZOS ? "Yeguarizos" : "Lanares"}</h2>
                        <div class="tabla-scroll tabla-planilla">
                            <table class="planilla">
                                <thead>
                                    <tr class="banda">
                                        <td colspan="2" class="lado"></td>
                                        <th scope="colgroup" colspan="5" class="ent">Entradas</th>
                                        <th scope="colgroup" colspan="7" class="sal">Salidas</th>
                                        <th scope="colgroup" colspan="2">Totales</th>
                                        <td class="lado final"></td>
                                    </tr>
                                    <tr>
                                        <th scope="col">Categoría</th>
                                        <th scope="col">Anterior</th>
                                        <th scope="col" class="ent${vacias[0] ? " col-vacia" : ""}">Compra</th>
                                        <th scope="col" class="ent${vacias[1] ? " col-vacia" : ""}">Nacimiento</th>
                                        <th scope="col" class="ent${vacias[2] ? " col-vacia" : ""}">Transf. DE</th>
                                        <th scope="col" class="ent${vacias[3] ? " col-vacia" : ""}">Cambio DE</th>
                                        <th scope="col" class="ent${vacias[4] ? " col-vacia" : ""}">Dif. recuento +</th>
                                        <th scope="col" class="sal${vacias[5] ? " col-vacia" : ""}">Venta</th>
                                        <th scope="col" class="sal${vacias[6] ? " col-vacia" : ""}">Mortandad</th>
                                        <th scope="col" class="sal${vacias[7] ? " col-vacia" : ""}">Consumo</th>
                                        <th scope="col" class="sal${vacias[8] ? " col-vacia" : ""}">Pastoreo</th>
                                        <th scope="col" class="sal${vacias[9] ? " col-vacia" : ""}">Transf. A</th>
                                        <th scope="col" class="sal${vacias[10] ? " col-vacia" : ""}">Cambio A</th>
                                        <th scope="col" class="sal${vacias[11] ? " col-vacia" : ""}">Dif. recuento -</th>
                                        <th scope="col" class="ent${vacias[12] ? " col-vacia" : ""}">Entradas</th>
                                        <th scope="col" class="sal${vacias[13] ? " col-vacia" : ""}">Salidas</th>
                                        <th scope="col" class="final">Final</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    @for (var fila : planilla.rows())
                                        @if (fila.category().species() == especie)
                                            <tr class="${fila.entradas() == 0 && fila.salidas() == 0 ? "sin-mov" : ""}">
                                                <td>${fila.category().name()}</td>
                                                <td>${fila.anterior()}</td>
                                                @template.fragment.celda(valor = fila.columns().get(Column.COMPRA), vacia = vacias[0])
                                                @template.fragment.celda(valor = fila.columns().get(Column.NACIMIENTO), vacia = vacias[1])
                                                @template.fragment.celda(valor = fila.columns().get(Column.TRASLADO_ENTRA), vacia = vacias[2])
                                                @template.fragment.celda(valor = fila.columns().get(Column.CAMBIO_ENTRA), vacia = vacias[3])
                                                @template.fragment.celda(valor = fila.columns().get(Column.RECUENTO_MAS), vacia = vacias[4])
                                                @template.fragment.celda(valor = fila.columns().get(Column.VENTA), vacia = vacias[5])
                                                @template.fragment.celda(valor = fila.columns().get(Column.MORTANDAD), vacia = vacias[6])
                                                @template.fragment.celda(valor = fila.columns().get(Column.CONSUMO), vacia = vacias[7])
                                                @template.fragment.celda(valor = fila.columns().get(Column.PASTOREO), vacia = vacias[8])
                                                @template.fragment.celda(valor = fila.columns().get(Column.TRASLADO_SALE), vacia = vacias[9])
                                                @template.fragment.celda(valor = fila.columns().get(Column.CAMBIO_SALE), vacia = vacias[10])
                                                @template.fragment.celda(valor = fila.columns().get(Column.RECUENTO_MENOS), vacia = vacias[11])
                                                @template.fragment.celda(valor = fila.entradas(), vacia = vacias[12])
                                                @template.fragment.celda(valor = fila.salidas(), vacia = vacias[13])
                                                <td><strong>${fila.finalStock()}</strong></td>
                                            </tr>
                                        @endif
                                    @endfor
                                    <tr>
                                        <td><strong>Total</strong></td>
                                        <td><strong>${planilla.totalFor(especie).anterior()}</strong></td>
                                        @template.fragment.celda(valor = planilla.totalFor(especie).columns().get(Column.COMPRA), vacia = vacias[0])
                                        @template.fragment.celda(valor = planilla.totalFor(especie).columns().get(Column.NACIMIENTO), vacia = vacias[1])
                                        @template.fragment.celda(valor = planilla.totalFor(especie).columns().get(Column.TRASLADO_ENTRA), vacia = vacias[2])
                                        @template.fragment.celda(valor = planilla.totalFor(especie).columns().get(Column.CAMBIO_ENTRA), vacia = vacias[3])
                                        @template.fragment.celda(valor = planilla.totalFor(especie).columns().get(Column.RECUENTO_MAS), vacia = vacias[4])
                                        @template.fragment.celda(valor = planilla.totalFor(especie).columns().get(Column.VENTA), vacia = vacias[5])
                                        @template.fragment.celda(valor = planilla.totalFor(especie).columns().get(Column.MORTANDAD), vacia = vacias[6])
                                        @template.fragment.celda(valor = planilla.totalFor(especie).columns().get(Column.CONSUMO), vacia = vacias[7])
                                        @template.fragment.celda(valor = planilla.totalFor(especie).columns().get(Column.PASTOREO), vacia = vacias[8])
                                        @template.fragment.celda(valor = planilla.totalFor(especie).columns().get(Column.TRASLADO_SALE), vacia = vacias[9])
                                        @template.fragment.celda(valor = planilla.totalFor(especie).columns().get(Column.CAMBIO_SALE), vacia = vacias[10])
                                        @template.fragment.celda(valor = planilla.totalFor(especie).columns().get(Column.RECUENTO_MENOS), vacia = vacias[11])
                                        @template.fragment.celda(valor = planilla.totalFor(especie).entradas(), vacia = vacias[12])
                                        @template.fragment.celda(valor = planilla.totalFor(especie).salidas(), vacia = vacias[13])
                                        <td><strong>${planilla.totalFor(especie).finalStock()}</strong></td>
                                    </tr>
                                </tbody>
                            </table>
                        </div>
                    @endif
                @endfor
                <p class="total-general">Total general: anterior <strong>${planilla.totalGeneral().anterior()}</strong>, entradas <strong>${planilla.totalGeneral().entradas()}</strong>, salidas <strong>${planilla.totalGeneral().salidas()}</strong>, final <strong>${planilla.totalGeneral().finalStock()}</strong></p>
                <dl class="leyenda">
                    <div><dt>Transf. DE</dt><dd>entran por traslado desde otro establecimiento</dd></div>
                    <div><dt>Transf. A</dt><dd>salen por traslado hacia otro establecimiento</dd></div>
                    <div><dt>Cambio DE</dt><dd>entran a la categoría por cambio de categoría</dd></div>
                    <div><dt>Cambio A</dt><dd>salen de la categoría por cambio de categoría</dd></div>
                    <div><dt>Dif. recuento +/-</dt><dd>ajustes por recuento, a favor o en contra</dd></div>
                </dl>
            @endif
        </main>
    </body>
</html>
