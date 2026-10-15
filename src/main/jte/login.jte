@param String error
@param org.springframework.security.web.csrf.CsrfToken _csrf
@param Boolean salio
<!DOCTYPE html>
<html lang="es">
    <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>Ingresar - CattleStock</title>
    </head>
    <body>
        <h1>CattleStock</h1>
        <p>Registro de movimientos de hacienda.</p>
        @if (error != null)
            <p class="error"><strong>${error}</strong></p>
        @endif
        @if (salio)
            <p>Cerraste sesion.</p>
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
            <p><button type="submit">Ingresar</button></p>
        </form>
    </body>
</html>
