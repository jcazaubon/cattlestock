@param String error
@param String establecimiento
@param String especie
@param String categoria
@param String fecha
@param java.util.List<com.kinetix.cattlestock.data.EstablishmentEntity> establecimientos
@param java.util.List<com.kinetix.cattlestock.domain.Ledger.ExistenceRow> filas
<!DOCTYPE html>
<html lang="es">
    <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>Existencia - CattleStock</title>
        <script src="/htmx.min.js"></script>
    </head>
    <body>
        <h1>Existencia</h1>
        <p><a href="/">Inicio</a> | <a href="/alta">Cargar movimiento</a> | <a href="/grilla">Carga rapida</a></p>
        @if (error != null)
            <p class="error"><strong>${error}</strong></p>
        @endif
        <form method="get" action="/existencia">
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
            <button type="submit">Ver</button>
        </form>
        <table border="1">
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
    </body>
</html>
