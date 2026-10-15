@param String appName
<!DOCTYPE html>
<html lang="es">
    <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>${appName}</title>
        <script src="/htmx.min.js"></script>
    </head>
    <body>
        <h1>${appName}</h1>
        <p>Registro de movimientos de hacienda.</p>
        <button hx-get="/fragment/ping" hx-target="#ping">Ping al servidor</button>
        <div id="ping"></div>
    </body>
</html>
