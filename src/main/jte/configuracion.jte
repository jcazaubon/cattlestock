@param String error
@param String aviso
@param String editar
@param String seccion
@param com.kinetix.cattlestock.data.CompanyEntity empresa
@param java.util.List<com.kinetix.cattlestock.data.EstablishmentEntity> establecimientos
@param String seleccionado
@param String seleccionadoNombre
@param java.util.List<com.kinetix.cattlestock.web.ConfiguracionController.FilaCategoria> filas
@param java.util.List<com.kinetix.cattlestock.data.CategoryEntity> categoriasEmpresa
@param java.util.List<com.kinetix.cattlestock.data.UserEntity> usuarios
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
        <title>Configuración - CattleStock</title>
        <link rel="stylesheet" href="/app.css">
        <meta name="htmx-config" content='{"globalViewTransitions":true}'>
        <script src="/htmx.min.js"></script>
    </head>
    <body>
        @template.fragment.barra(usuario = usuario, esAdmin = esAdmin, esSuperadmin = esSuperadmin, empresaNombre = empresaNombre, empresasMenu = empresasMenu, empresaActivaId = empresaActivaId, ruta = ruta, _csrf = _csrf)
        <main class="contenedor config">
            <h1>Configuración</h1>
            @if (empresa != null)
                <p class="subtitulo">${empresa.getName()}, arranca en ${empresa.getStartMonth()}</p>
            @else
                <p class="vacio">Todavía no hay una empresa. La da de alta el superadmin en Administración.</p>
            @endif

            @if (error != null)
                <p class="error"><strong>${error}</strong></p>
            @endif
            @if (aviso != null)
                <p class="aviso"><strong>${aviso}</strong></p>
            @endif

            <nav class="pestanas" aria-label="Secciones de la configuracion">
                <a href="/configuracion?seccion=establecimientos" aria-current="${seccion.equals("establecimientos") ? "page" : null}">Establecimientos<span class="cuenta">${establecimientos.size()}</span></a>
                <a href="/configuracion?seccion=categorias" aria-current="${seccion.equals("categorias") ? "page" : null}">Categorías<span class="cuenta">${categoriasEmpresa.size()}</span></a>
                <a href="/configuracion?seccion=stock" aria-current="${seccion.equals("stock") ? "page" : null}">Stock inicial</a>
                <a href="/configuracion?seccion=usuarios" aria-current="${seccion.equals("usuarios") ? "page" : null}">Usuarios<span class="cuenta">${usuarios.size()}</span></a>
            </nav>

            @if (seccion.equals("establecimientos"))
                <section>
                    <h2>Establecimientos</h2>
                    <form method="post" action="/configuracion/establecimientos" class="filtros">
                        @if (_csrf != null)
                            <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                        @endif
                        <label>Nombre<input name="nombre" required></label>
                        <label>Código corto (opcional)<input name="codigo" placeholder="se genera solo"></label>
                        <button type="submit" class="boton">Agregar establecimiento</button>
                    </form>

                    @if (establecimientos.isEmpty())
                        <p class="vacio">Todavía no hay establecimientos. Agrega el primero arriba.</p>
                    @else
                        <div class="tabla-scroll">
                            <table>
                                <thead>
                                    <tr><th>Nombre</th><th>Código</th><th></th></tr>
                                </thead>
                                <tbody>
                                    @for (var est : establecimientos)
                                        <tr class="${est.getCode().equals(seleccionado) ? "elegida" : ""}">
                                            <td>${est.getName()}</td>
                                            <td class="codigo">${est.getCode()}</td>
                                            <td class="accion"><a href="/configuracion?establecimiento=${est.getCode()}">Editar y stock</a></td>
                                        </tr>
                                    @endfor
                                </tbody>
                            </table>
                        </div>
                    @endif

                    @if (!seleccionado.isEmpty())
                        <div class="panel-detalle">
                            <h3>${seleccionadoNombre}</h3>
                            <form method="post" action="/configuracion/establecimientos/${seleccionado}" class="filtros">
                                @if (_csrf != null)
                                    <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                                @endif
                                <label>Nombre<input name="nombre" value="${seleccionadoNombre}" required></label>
                                <button type="submit" class="boton boton-linea">Guardar nombre</button>
                            </form>
                            <p class="ayuda">El código corto (${seleccionado}) no se cambia: es el que usan las planillas, los movimientos y los usuarios.</p>

                            <h4>Stock inicial</h4>
                            @if (filas.isEmpty())
                                <p class="vacio">La empresa todavía no tiene categorías. Agregalas en la pestaña Categorías.</p>
                            @else
                                <form method="post" action="/configuracion/stock">
                                    @if (_csrf != null)
                                        <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                                    @endif
                                    <input type="hidden" name="establecimiento" value="${seleccionado}">
                                    <div class="tabla-scroll">
                                        <table class="stock">
                                            <thead>
                                                <tr><th>Categoría</th><th>Cantidad</th></tr>
                                            </thead>
                                            <tbody>
                                                @for (var f : filas)
                                                    <tr>
                                                        <td>${f.categoria().getName()}</td>
                                                        <td><input type="number" name="stock_${f.categoria().getCode()}" min="0"
                                                                aria-label="Stock inicial de ${f.categoria().getName()}"
                                                                value="${f.cantidad() >= 0 ? String.valueOf(f.cantidad()) : ""}"></td>
                                                    </tr>
                                                @endfor
                                            </tbody>
                                        </table>
                                    </div>
                                    <p><button type="submit" class="boton">Guardar stock inicial</button></p>
                                </form>
                            @endif
                        </div>
                    @endif
                </section>
            @endif

            @if (seccion.equals("categorias"))
                <section>
                    <h2>Categorías</h2>
                    <p class="ayuda">Son de la empresa: las usan todos sus establecimientos.</p>
                    <form method="post" action="/configuracion/categorias" class="filtros">
                        @if (_csrf != null)
                            <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                        @endif
                        <label>Nombre<input name="nombre" required></label>
                        <label>Especie
                            <select name="especie">
                                <option value="VACUNOS">Vacunos</option>
                                <option value="YEGUARIZOS">Yeguarizos</option>
                                <option value="LANARES">Lanares</option>
                            </select>
                        </label>
                        <label>Código (opcional)<input name="codigo" placeholder="se genera solo"></label>
                        <button type="submit" class="boton">Agregar categoría</button>
                    </form>

                    <details class="importar">
                        <summary>Importar desde una planilla o un texto</summary>
                        <p class="ayuda">Subí una planilla de hacienda en Excel (.xlsx o .xls) o un archivo de texto, o pegá las categorías una por linea. Antes de crear nada vas a ver lo que se detecto y podés sacar o corregir.</p>
                        <form method="post" action="/configuracion/categorias/importar?${_csrf == null ? "" : _csrf.getParameterName() + "=" + _csrf.getToken()}"
                              enctype="multipart/form-data" class="filtros">
                            <label>Archivo<input type="file" name="archivo" accept=".xlsx,.xls,.xlsm,.csv,.txt"></label>
                            <label>O pegalas acá<textarea name="texto" rows="3" placeholder="Vacas Plantel&#10;Toros&#10;Terneros orejanos"></textarea></label>
                            <button type="submit" class="boton">Ver lo que se detecta</button>
                        </form>
                    </details>

                    @template.fragment.tablaCategorias(categoriasEmpresa = categoriasEmpresa, editar = editar, movida = "", error = null, _csrf = _csrf)
                </section>
            @endif

            @if (seccion.equals("stock"))
                <section>
                    <h2>Stock inicial</h2>
                    <p class="ayuda">Carga el stock inicial de todos los establecimientos de una vez con una planilla de Excel. También sirve para corregirlo: la planilla ya trae lo que cargaste.</p>
                    @if (establecimientos.isEmpty() || categoriasEmpresa.isEmpty())
                        <p class="vacio">Primero hacen falta establecimientos y categorías. Agregalos en esas pestanas.</p>
                    @else
                        <ol class="pasos">
                            <li>
                                <strong>Descarga la planilla.</strong> Trae las ${categoriasEmpresa.size()} categorías de la empresa en las filas y un establecimiento por columna.
                                <div class="acciones"><a class="boton boton-linea" href="/configuracion/stock/plantilla">Descargar planilla de Excel</a></div>
                            </li>
                            <li><strong>Completa las cantidades.</strong> Una celda vacia no cambia nada y un 0 deja el stock en cero. No cambies los nombres ni los encabezados.</li>
                            <li>
                                <strong>Subila.</strong> Antes de aplicar nada vas a ver lo que cambia.
                                <form method="post" action="/configuracion/stock/importar?${_csrf == null ? "" : _csrf.getParameterName() + "=" + _csrf.getToken()}"
                                      enctype="multipart/form-data" class="filtros">
                                    <label>Archivo<input type="file" name="archivo" accept=".xlsx,.xls,.xlsm" required></label>
                                    <button type="submit" class="boton">Ver lo que cambia</button>
                                </form>
                            </li>
                        </ol>
                    @endif
                </section>
            @endif

            @if (seccion.equals("usuarios"))
                <section>
                    <h2>Usuarios</h2>
                    <p class="ayuda">Cada usuario opera solo en los establecimientos que tenga tildados. Un administrador sin ninguno tildado ve todos los de la empresa. Acá, en Configuración, el administrador siempre ve todos para poder administrarlos.</p>

                    <form method="post" action="/configuracion/usuarios" class="filtros">
                        @if (_csrf != null)
                            <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                        @endif
                        <label>Usuario<input name="username" required></label>
                        <label>Contraseña<input type="password" name="password" required></label>
                        <label>Rol
                            <select name="rol">
                                <option value="CARGA">Carga</option>
                                <option value="ADMIN">Administrador</option>
                            </select>
                        </label>
                        <div class="fila-completa">
                            <span class="rotulo-fila">Establecimientos asignados</span>
                            <span class="opciones">
                                @for (var est : establecimientos)
                                    <label><input type="checkbox" name="establecimientos" value="${est.getCode()}"> ${est.getName()}</label>
                                @endfor
                            </span>
                            <button type="submit" class="boton">Agregar usuario</button>
                        </div>
                    </form>

                    @if (usuarios.isEmpty())
                        <p class="vacio">Todavía no hay usuarios.</p>
                    @else
                        <div class="tabla-scroll">
                            <table class="usuarios">
                                <thead>
                                    <tr><th>Usuario</th><th>Rol y establecimientos</th></tr>
                                </thead>
                                <tbody>
                                    @for (var u : usuarios)
                                        <tr>
                                            <td>${u.getUsername()}</td>
                                            <td>
                                                <form method="post" action="/configuracion/usuarios/${u.getId()}" class="usuario-fila">
                                                    @if (_csrf != null)
                                                        <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                                                    @endif
                                                    <select name="rol" aria-label="Rol de ${u.getUsername()}">
                                                        <option value="CARGA" selected="${u.getRole().name().equals("CARGA")}">Carga</option>
                                                        <option value="ADMIN" selected="${u.getRole().name().equals("ADMIN")}">Administrador</option>
                                                    </select>
                                                    <span class="opciones">
                                                        @for (var est : establecimientos)
                                                            <label><input type="checkbox" name="establecimientos" value="${est.getCode()}"
                                                                    checked="${u.getEstablishments().stream().anyMatch(x -> x.getCode().equals(est.getCode()))}"> ${est.getName()}</label>
                                                        @endfor
                                                    </span>
                                                    <button type="submit" class="boton boton-linea">Guardar</button>
                                                </form>
                                            </td>
                                        </tr>
                                    @endfor
                                </tbody>
                            </table>
                        </div>
                    @endif
                </section>
            @endif
        </main>
    </body>
</html>
