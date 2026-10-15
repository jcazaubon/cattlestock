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
@param org.springframework.security.web.csrf.CsrfToken _csrf
<!DOCTYPE html>
<html lang="es">
    <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>Cargar movimiento - CattleStock</title>
        <script src="/htmx.min.js"></script>
    </head>
    <body>
        <h1>Cargar movimiento</h1>
        <p><a href="/">Inicio</a></p>
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
                <label>Establecimiento<br>
                    <select name="establecimiento" required
                            hx-get="/alta/categorias" hx-target="#categorias" hx-include="this">
                        <option value=""></option>
                        @for (var est : establecimientos)
                            <option value="${est.getCode()}" selected="${est.getCode().equals(establecimiento)}">${est.getName()}</option>
                        @endfor
                    </select>
                </label>
            </p>
            <p id="categorias">
                @template.fragment.categorias(categorias = categorias, categoria = categoria)
            </p>
            <p>
                <label>Tipo<br>
                    <select name="tipo" required
                            hx-get="/alta/campos" hx-target="#campos"
                            hx-include="[name='establecimiento'], [name='categoria'], [name='destinoEstablecimiento'], this">
                        <option value=""></option>
                        <option value="COMPRA" selected="${"COMPRA".equals(tipo)}">Compra</option>
                        <option value="VENTA" selected="${"VENTA".equals(tipo)}">Venta</option>
                        <option value="NACIMIENTO" selected="${"NACIMIENTO".equals(tipo)}">Nacimiento o produccion</option>
                        <option value="MORTANDAD" selected="${"MORTANDAD".equals(tipo)}">Mortandad</option>
                        <option value="CONSUMO" selected="${"CONSUMO".equals(tipo)}">Consumo</option>
                        <option value="TRASLADO" selected="${"TRASLADO".equals(tipo)}">Traslado</option>
                        <option value="CAMBIO_CATEGORIA" selected="${"CAMBIO_CATEGORIA".equals(tipo)}">Cambio de categoria</option>
                    </select>
                </label>
            </p>
            <div id="campos">
                @template.fragment.campos(tipo = tipo, establecimiento = establecimiento, categoria = categoria,
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
            <p><button type="submit">Guardar</button></p>
        </form>
    </body>
</html>
