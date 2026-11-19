@param String error
@param boolean forzada
@param org.springframework.security.web.csrf.CsrfToken _csrf
<!DOCTYPE html>
<html lang="es">
    <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>Cambiar contraseña - CattleStock</title>
        <link rel="stylesheet" href="/app.css">
    </head>
    <body class="login">
        <main class="tarjeta-login">
            <a class="marca" href="/"><span class="caravana" aria-hidden="true"></span>CattleStock</a>
            <h1>Cambiar contraseña</h1>
            @if (forzada)
                <p class="introduccion">Estás usando la contraseña que te dio el administrador. Elegí una propia para seguir.</p>
            @else
                <p class="introduccion">Elegí una contraseña nueva.</p>
            @endif
            @if (error != null)
                <p class="error"><strong>${error}</strong></p>
            @endif
            <form method="post" action="/cuenta/clave">
                @if (_csrf != null)
                    <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                @endif
                <p><label>Contraseña actual<br><input type="password" name="actual" required autocomplete="current-password" autofocus></label></p>
                <p><label>Contraseña nueva<br><input type="password" name="nueva" required minlength="8" autocomplete="new-password"></label></p>
                <p><label>Repetí la contraseña nueva<br><input type="password" name="repetir" required minlength="8" autocomplete="new-password"></label></p>
                <p class="ayuda">Al menos 8 caracteres y distinta del usuario.</p>
                <p><button type="submit" class="boton">Guardar contraseña</button></p>
            </form>
            @if (forzada)
                <form method="post" action="/logout">
                    @if (_csrf != null)
                        <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                    @endif
                    <button type="submit" class="boton boton-linea">Salir</button>
                </form>
            @else
                <p><a href="/">Volver</a></p>
            @endif
        </main>
    </body>
</html>
