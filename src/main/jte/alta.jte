@param String error
@param String hoy
@param String establecimiento
@param String categoria
@param String tipo
@param String fecha
@param String cantidad
@param String cueros
@param String nota
@param String destinoEstablecimiento
@param String destinoCategoria
@param java.util.List<com.kinetix.cattlestock.data.EstablishmentEntity> establecimientos
@param java.util.List<com.kinetix.cattlestock.data.CategoryEntity> categorias
@param java.util.List<com.kinetix.cattlestock.data.CategoryEntity> destinoCategorias
@param String usuario
@param boolean esAdmin
@param org.springframework.security.web.csrf.CsrfToken _csrf
<!DOCTYPE html>
<html lang="es">
    <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>Cargar movimiento - CattleStock</title>
        <link rel="stylesheet" href="/app.css">
        <meta name="htmx-config" content='{"globalViewTransitions":true}'>
        <script src="/htmx.min.js"></script>
    </head>
    <body>
        <header class="barra">
            <a class="marca" href="/"><span class="caravana" aria-hidden="true"></span>CattleStock</a>
            <nav hx-boost="true">
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
            <h1>Cargar movimiento</h1>
            @if (error != null)
                <p class="error"><strong>${error}</strong></p>
            @endif
            <form method="post" action="/movimientos">
                @if (_csrf != null)
                    <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                @endif
                <p>
                    <label>Fecha<br>
                        <input type="date" name="fecha" value="${fecha.isEmpty() ? hoy : fecha}" required>
                    </label>
                </p>
                <p>
                    <label>Establecimiento de origen<br>
                        <select name="establecimiento" required
                                hx-get="/alta/origen" hx-target="#segun-origen"
                                hx-include="[name='tipo'], [name='categoria'], [name='destinoEstablecimiento'], [name='destinoCategoria'], this">
                            <option value=""></option>
                            @for (var est : establecimientos)
                                <option value="${est.getCode()}" selected="${est.getCode().equals(establecimiento)}">${est.getName()}</option>
                            @endfor
                        </select>
                    </label>
                </p>
                <div id="segun-origen">
                    @template.fragment.origen(establecimiento = establecimiento, categoria = categoria, tipo = tipo,
                            destinoEstablecimiento = destinoEstablecimiento, destinoCategoria = destinoCategoria,
                            establecimientos = establecimientos, categorias = categorias, destinoCategorias = destinoCategorias)
                </div>
                <p>
                    <label>Cantidad<br>
                        <input type="number" name="cantidad" min="1" value="${cantidad}" required>
                    </label>
                </p>
                <p>
                    <label>Nota<br>
                        <input type="text" name="nota" value="${nota}">
                    </label>
                </p>
                <p><button type="submit" class="boton">Guardar</button></p>
            </form>
        </main>
    </body>
</html>
