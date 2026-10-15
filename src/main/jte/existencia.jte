@param String error
@param String establecimiento
@param String especie
@param String categoria
@param String fecha
@param java.util.List<com.kinetix.cattlestock.data.EstablishmentEntity> establecimientos
@param java.util.List<com.kinetix.cattlestock.domain.Ledger.ExistenceRow> filas
@param String usuario
@param boolean esAdmin
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
        <header class="barra">
            <a class="marca" href="/"><span class="caravana" aria-hidden="true"></span>CattleStock</a>
            <nav hx-boost="true">
                <a href="/alta">Cargar movimiento</a>
                <a href="/grilla">Carga rapida</a>
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
            <h1>Existencia</h1>
            <form method="get" action="/existencia" class="filtros">
                <label>Establecimiento
                    <select name="establecimiento">
                        <option value="">Todos</option>
                        @for (var est : establecimientos)
                            <option value="${est.getCode()}" selected="${est.getCode().equals(establecimiento)}">${est.getName()}</option>
                        @endfor
                    </select>
                </label>
                <label>Especie
                    <select name="especie">
                        <option value="">Todas</option>
                        <option value="VACUNOS" selected="${"VACUNOS".equals(especie)}">Vacunos</option>
                        <option value="YEGUARIZOS" selected="${"YEGUARIZOS".equals(especie)}">Yeguarizos</option>
                        <option value="LANARES" selected="${"LANARES".equals(especie)}">Lanares</option>
                    </select>
                </label>
                <label>Categoria <input type="text" name="categoria" value="${categoria}"></label>
                <label>Fecha <input type="date" name="fecha" value="${fecha}"></label>
                <button type="submit" class="boton boton-linea">Ver</button>
            </form>
            @if (error != null)
                <p class="error"><strong>${error}</strong></p>
            @endif
            <div class="tabla-scroll">
                <table class="existencia">
                    <thead>
                        <tr>
                            <th>Establecimiento</th>
                            <th>Categoria</th>
                            <th>Especie</th>
                            <th>Cantidad</th>
                        </tr>
                    </thead>
                    <tbody>
                        @for (var fila : filas)
                            <tr>
                                <td>${fila.establishment().name()}</td>
                                <td>${fila.category().name()}</td>
                                <td>${fila.category().species()}</td>
                                <td><strong>${fila.quantity()}</strong></td>
                            </tr>
                        @endfor
                    </tbody>
                </table>
            </div>
        </main>
    </body>
</html>
