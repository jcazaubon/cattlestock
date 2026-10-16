@param String error
@param String hoy
@param String establecimiento
@param String fecha
@param String nota
@param java.util.Map<String, String> valores
@param java.util.List<com.kinetix.cattlestock.data.EstablishmentEntity> establecimientos
@param java.util.List<com.kinetix.cattlestock.data.CategoryEntity> categorias
@param String usuario
@param boolean esAdmin
@param org.springframework.security.web.csrf.CsrfToken _csrf
<!DOCTYPE html>
<html lang="es">
    <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>Carga rapida - CattleStock</title>
        <link rel="stylesheet" href="/app.css">
        <meta name="htmx-config" content='{"globalViewTransitions":true}'>
        <script src="/htmx.min.js"></script>
    </head>
    <body>
        <header class="barra">
            <a class="marca" href="/"><span class="caravana" aria-hidden="true"></span>CattleStock</a>
            <nav hx-boost="true">
                <a href="/alta">Cargar movimiento</a>
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
            <h1>Carga rapida en grilla</h1>
            @if (error != null)
                <p class="error"><strong>${error}</strong></p>
            @endif
            <p class="introduccion">Una fila por categoria, una fecha para todo el lote. Se cargan solo las celdas con numero. Traslados y cambios de categoria van por el formulario de alta, porque necesitan destino.</p>
            <form method="post" action="/grilla">
                @if (_csrf != null)
                    <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                @endif
                <p>
                    <label>Fecha<br>
                        <input type="date" name="fecha" value="${fecha.isEmpty() ? hoy : fecha}" required>
                    </label>
                </p>
                <p>
                    <label>Establecimiento<br>
                        <select name="establecimiento" required
                                hx-get="/grilla/filas" hx-target="#filas" hx-include="closest form">
                            <option value=""></option>
                            @for (var est : establecimientos)
                                <option value="${est.getCode()}" selected="${est.getCode().equals(establecimiento)}">${est.getName()}</option>
                            @endfor
                        </select>
                    </label>
                </p>
                <div class="tabla-scroll">
                    <table class="grilla">
                        <thead>
                            <tr>
                                <th>Categoria</th>
                                <th>Compra</th>
                                <th>Nacimiento</th>
                                <th>Venta</th>
                                <th>Mortandad</th>
                                <th>Cueros</th>
                                <th>Consumo</th>
                            </tr>
                        </thead>
                        <tbody id="filas">
                            @template.fragment.filasGrilla(categorias = categorias, valores = valores)
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
