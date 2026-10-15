@param String error
@param org.springframework.security.web.csrf.CsrfToken _csrf
@param Boolean salio
<!DOCTYPE html>
<html lang="es">
    <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>Ingresar - CattleStock</title>
        <link rel="stylesheet" href="/app.css">
    </head>
    <body class="login">
        <main class="tarjeta-login">
            <a class="marca" href="/"><span class="caravana" aria-hidden="true"></span>CattleStock</a>
            <p class="introduccion">Registro de movimientos de hacienda.</p>
            @if (error != null)
                <p class="error"><strong>${error}</strong></p>
            @endif
            @if (salio)
                <p class="aviso">Cerraste sesion.</p>
            @endif
            <form method="post" action="/login">
                @if (_csrf != null)
                    <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                @endif
                <p>
                    <label>Usuario<br><input name="username" required autofocus></label>
                </p>
                <p>
                    <label>Password<br><input type="password" name="password" required></label>
                </p>
                <p><button type="submit" class="boton">Ingresar</button></p>
            </form>
        </main>
    </body>
</html>
