@param int codigo
@param String titulo
@param String detalle
<!DOCTYPE html>
<html lang="es">
    <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>Error - CattleStock</title>
        <link rel="stylesheet" href="/app.css">
    </head>
    <body>
        <main class="tarjeta-error">
            <p class="codigo">${codigo}</p>
            <h1>${titulo}</h1>
            <p class="introduccion">${detalle}</p>
            <p>
                @if (codigo == 403)
                    <a class="boton" href="/login">Iniciar sesion</a>
                @endif
                <a class="boton boton-linea" href="/">Volver al inicio</a>
            </p>
        </main>
    </body>
</html>
