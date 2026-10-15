@param String error
@param String hoy
@param String establecimiento
@param String fecha
@param String nota
@param java.util.Map<String, String> valores
@param java.util.List<com.kinetix.cattlestock.data.EstablishmentEntity> establecimientos
@param java.util.List<com.kinetix.cattlestock.data.CategoryEntity> categorias
<!DOCTYPE html>
<html lang="es">
    <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>Carga rapida - CattleStock</title>
        <script src="/htmx.min.js"></script>
    </head>
    <body>
        <h1>Carga rapida en grilla</h1>
        <p><a href="/">Inicio</a> | <a href="/alta">Cargar un movimiento</a></p>
        @if (error != null)
            <p class="error"><strong>${error}</strong></p>
        @endif
        <form method="post" action="/grilla">
            <p>
                <label>Fecha<br>
                    <input type="date" name="fecha" value="${fecha.isEmpty() ? hoy : fecha}" required>
                </label>
            </p>
            <p>
                <label>Establecimiento<br>
                    <select name="establecimiento" required
                            hx-get="/grilla/filas" hx-target="#filas" hx-include="this">
                        <option value=""></option>
                        @for (var est : establecimientos)
                            <option value="${est.getCode()}" selected="${est.getCode().equals(establecimiento)}">${est.getName()}</option>
                        @endfor
                    </select>
                </label>
            </p>
            <p>Una fila por categoria. Se cargan solo las celdas con numero. Traslados y cambios de categoria van por el formulario de alta.</p>
            <table border="1">
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
            <p>
                <label>Nota (para todo el lote)<br>
                    <input type="text" name="nota" value="${nota}">
                </label>
            </p>
            <p><button type="submit">Guardar lote</button></p>
        </form>
    </body>
</html>
