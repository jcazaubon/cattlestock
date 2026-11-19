@param java.util.List<com.kinetix.cattlestock.web.HistorialController.Fila> filas
@param java.util.List<com.kinetix.cattlestock.data.EstablishmentEntity> establecimientos
@param String establecimiento
@param String mes
@param String cat
@param String tipo
@param boolean eliminados
@param String editar
@param java.util.List<com.kinetix.cattlestock.data.CategoryEntity> categorias
@param java.util.List<com.kinetix.cattlestock.web.HistorialController.Opcion> tipos
@param String error
@param String aviso
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
        <title>Historial de movimientos - CattleStock</title>
        <link rel="stylesheet" href="/app.css">
        <meta name="htmx-config" content='{"globalViewTransitions":true}'>
        <script src="/htmx.min.js"></script>
    </head>
    <body>
        @template.fragment.barra(usuario = usuario, esAdmin = esAdmin, esSuperadmin = esSuperadmin, empresaNombre = empresaNombre, empresasMenu = empresasMenu, empresaActivaId = empresaActivaId, ruta = ruta, _csrf = _csrf)
        <main class="contenedor">
            <h1>Historial de movimientos</h1>
            <p class="subtitulo">Corregi o elimina movimientos cargados. Un movimiento eliminado queda en el historial y se puede restaurar.</p>

            <form method="get" action="/historial" class="filtros">
                <label>Establecimiento
                    <select name="establecimiento">
                        <option value="">Todos</option>
                        @for (var est : establecimientos)
                            <option value="${est.getCode()}" selected="${est.getCode().equals(establecimiento)}">${est.getName()}</option>
                        @endfor
                    </select>
                </label>
                @template.fragment.periodo(nombre = "mes", valor = mes)
                <label>Categoría
                    <select name="cat">
                        <option value="">Todas</option>
                        @for (var c : categorias)
                            <option value="${c.getCode()}" selected="${c.getCode().equals(cat)}">${c.getName()}</option>
                        @endfor
                    </select>
                </label>
                <label>Tipo
                    <select name="tipo">
                        <option value="">Todos</option>
                        @for (var t : tipos)
                            <option value="${t.valor()}" selected="${t.valor().equals(tipo)}">${t.etiqueta()}</option>
                        @endfor
                    </select>
                </label>
                <label class="opcion-inline"><input type="checkbox" name="eliminados" value="1" checked="${eliminados}"> Mostrar eliminados</label>
                <button type="submit" class="boton boton-linea">Ver</button>
            </form>

            @if (error != null)
                <p class="error"><strong>${error}</strong></p>
            @endif
            @if (aviso != null)
                <p class="aviso"><strong>${aviso}</strong></p>
            @endif

            @if (filas.isEmpty())
                <p class="vacio">No hay movimientos con esos filtros.</p>
            @else
                <p class="periodo">${filas.size() == 1 ? "1 movimiento" : filas.size() + " movimientos"}</p>
                <div class="tabla-scroll">
                    <table class="historial">
                        <thead>
                            <tr>
                                <th>Fecha</th>
                                <th>Establecimiento</th>
                                <th>Tipo</th>
                                <th>Categoría</th>
                                <th class="num">Cantidad</th>
                                <th>Destino</th>
                                <th>Nota</th>
                                <th></th>
                            </tr>
                        </thead>
                        <tbody>
                            @for (var f : filas)
                                @if (String.valueOf(f.id()).equals(editar) && !f.eliminado())
                                    <tr class="elegida">
                                        <td>
                                            <form id="editar" method="post" action="/historial/${f.id()}/editar" class="edicion-fila">
                                                @if (_csrf != null)
                                                    <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                                                @endif
                                                @template.fragment.filtrosOcultos(establecimiento = establecimiento, mes = mes, cat = cat, tipo = tipo, eliminados = eliminados)
                                            </form>
                                            <input type="date" name="fecha" form="editar" value="${f.fechaIso()}" required aria-label="Fecha">
                                        </td>
                                        <td>${f.establecimiento()}</td>
                                        <td>${f.tipo()}</td>
                                        <td>
                                            <select name="categoria" form="editar" aria-label="Categoría">
                                                @for (var c : categorias)
                                                    <option value="${c.getCode()}" selected="${c.getCode().equals(f.categoriaCodigo())}">${c.getName()}</option>
                                                @endfor
                                            </select>
                                        </td>
                                        <td class="num">
                                            <input type="number" name="cantidad" form="editar" min="1" value="${f.cantidad()}" required aria-label="Cantidad">
                                            @if (f.mortandad())
                                                <input type="number" name="cueros" form="editar" min="0" value="${f.cueros() == null ? "" : String.valueOf(f.cueros())}" aria-label="Cueros" title="Cueros" placeholder="Cueros">
                                            @endif
                                        </td>
                                        <td>
                                            @if (f.conDestino())
                                                <select name="destinoCategoria" form="editar" aria-label="Categoría de destino">
                                                    @for (var c : categorias)
                                                        <option value="${c.getCode()}" selected="${c.getCode().equals(f.destinoCategoriaCodigo())}">${c.getName()}</option>
                                                    @endfor
                                                </select>
                                            @endif
                                        </td>
                                        <td><input name="nota" form="editar" value="${f.nota()}" aria-label="Nota"></td>
                                        <td class="botones">
                                            <button type="submit" form="editar" class="boton">Guardar</button>
                                            <form method="get" action="/historial">
                                                @template.fragment.filtrosOcultos(establecimiento = establecimiento, mes = mes, cat = cat, tipo = tipo, eliminados = eliminados)
                                                <button type="submit" class="boton boton-linea">Cancelar</button>
                                            </form>
                                        </td>
                                    </tr>
                                @else
                                    <tr class="${f.eliminado() ? "eliminado" : ""}">
                                        <td>${f.fecha()}</td>
                                        <td>${f.establecimiento()}</td>
                                        <td>${f.tipo()}</td>
                                        <td>${f.categoria()}</td>
                                        <td class="num">${f.cantidad()}${f.mortandad() && f.cueros() != null ? " (" + f.cueros() + " cueros)" : ""}</td>
                                        <td>${f.destino()}</td>
                                        <td>
                                            ${f.nota()}
                                            @if (!f.auditoria().isEmpty())
                                                <span class="auditoria">${f.auditoria()}</span>
                                            @endif
                                        </td>
                                        <td class="botones">
                                            @if (f.eliminado())
                                                <form method="post" action="/historial/${f.id()}/restaurar">
                                                    @if (_csrf != null)
                                                        <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                                                    @endif
                                                    @template.fragment.filtrosOcultos(establecimiento = establecimiento, mes = mes, cat = cat, tipo = tipo, eliminados = eliminados)
                                                    <button type="submit" class="boton boton-linea">Restaurar</button>
                                                </form>
                                            @else
                                                <form method="get" action="/historial">
                                                    @template.fragment.filtrosOcultos(establecimiento = establecimiento, mes = mes, cat = cat, tipo = tipo, eliminados = eliminados)
                                                    <input type="hidden" name="editar" value="${f.id()}">
                                                    <button type="submit" class="boton boton-linea">Editar</button>
                                                </form>
                                                <form method="post" action="/historial/${f.id()}/eliminar"
                                                      data-confirmar="Queda en el historial y se puede restaurar." data-confirmar-titulo="Eliminar el movimiento"
                                                      data-confirmar-boton="Eliminar" data-confirmar-peligro="si">
                                                    @if (_csrf != null)
                                                        <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                                                    @endif
                                                    @template.fragment.filtrosOcultos(establecimiento = establecimiento, mes = mes, cat = cat, tipo = tipo, eliminados = eliminados)
                                                    <button type="submit" class="boton boton-peligro">Eliminar</button>
                                                </form>
                                            @endif
                                        </td>
                                    </tr>
                                @endif
                            @endfor
                        </tbody>
                    </table>
                </div>
            @endif
        </main>
    </body>
</html>
