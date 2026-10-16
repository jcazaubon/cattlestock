@param String error
@param String hoy
@param String establecimiento
@param String fecha
@param String nota
@param String usuario
@param boolean esAdmin
@param org.springframework.security.web.csrf.CsrfToken _csrf
@param java.util.List<com.kinetix.cattlestock.data.EstablishmentEntity> establecimientos
@param java.util.List<com.kinetix.cattlestock.data.CategoryEntity> categorias
@param java.util.List<com.kinetix.cattlestock.web.AltaController.LineaForm> lineas
@param java.util.Map<java.lang.Integer, java.util.List<com.kinetix.cattlestock.data.CategoryEntity>> destinoCategoriasPorIndice
<!DOCTYPE html>
<html lang="es">
    <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>Cargar movimientos - CattleStock</title>
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
            <h1>Cargar movimientos</h1>
            <p class="introduccion">Una linea por movimiento, un solo guardado: el lote entra completo o no entra nada. Traslados y cambios de categoria llevan su destino en la misma linea, por partida doble.</p>
            @if (error != null)
                <p class="error"><strong>${error}</strong></p>
            @endif
            <form method="post" action="/movimientos">
                @if (_csrf != null)
                    <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                @endif
                <div class="lote-cabecera">
                    <label>Fecha
                        <input type="date" name="fecha" value="${fecha.isEmpty() ? hoy : fecha}" required>
                    </label>
                    <label>Establecimiento
                        <select name="establecimiento" required
                                hx-get="/alta/lineas" hx-target="#lineas" hx-include="closest form">
                            <option value=""></option>
                            @for (var est : establecimientos)
                                <option value="${est.getCode()}" selected="${est.getCode().equals(establecimiento)}">${est.getName()}</option>
                            @endfor
                        </select>
                    </label>
                </div>
                <div id="lineas">
                    @template.fragment.lineas(lineas = lineas, categorias = categorias,
                            establecimientos = establecimientos, establecimiento = establecimiento,
                            destinoCategoriasPorIndice = destinoCategoriasPorIndice)
                </div>
                <div class="lote-pie">
                    <label class="nota-lote">Nota del lote
                        <input type="text" name="nota" value="${nota}">
                    </label>
                    <button type="button" class="boton boton-linea"
                            hx-get="/alta/linea" hx-target="#lineas" hx-swap="beforeend" hx-include="closest form">
                        Agregar linea
                    </button>
                    <button type="submit" class="boton">Guardar lote</button>
                </div>
            </form>
        </main>
    </body>
</html>
