@param String error
@param java.util.List<com.kinetix.cattlestock.web.AdminController.FilaEmpresa> empresas
@param java.util.List<com.kinetix.cattlestock.data.UserEntity> superadmins
@param String usuario
@param boolean esAdmin
@param boolean esSuperadmin
@param String empresaNombre
@param java.util.List<com.kinetix.cattlestock.data.CompanyEntity> empresasMenu
@param Long empresaActivaId
@param String ruta
@param org.springframework.security.web.csrf.CsrfToken _csrf
<!DOCTYPE html>
<html lang="es">
    <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>Administración - CattleStock</title>
        <link rel="stylesheet" href="/app.css">
        <meta name="htmx-config" content='{"globalViewTransitions":true}'>
        <script src="/htmx.min.js"></script>
    </head>
    <body>
        @template.fragment.barra(usuario = usuario, esAdmin = esAdmin, esSuperadmin = esSuperadmin, empresaNombre = empresaNombre, empresasMenu = empresasMenu, empresaActivaId = empresaActivaId, ruta = ruta, _csrf = _csrf)
        <main class="contenedor">
            <h1>Administración</h1>
            <p class="introduccion">Das de alta las empresas y sus administradores. Cada administrador gestiona solo su empresa; vos las ves todas y trabajas en la que elijas.</p>
            @if (error != null)
                <p class="error"><strong>${error}</strong></p>
            @endif

            <h2>Empresas</h2>
            @if (empresas.isEmpty())
                <p class="vacio">Todavía no hay empresas. Crea la primera abajo.</p>
            @else
                <div class="tabla-scroll">
                    <table class="admin-tabla">
                        <thead>
                            <tr>
                                <th>Empresa</th>
                                <th>Arranca en</th>
                                <th>Establecimientos</th>
                                <th>Usuarios</th>
                                <th></th>
                            </tr>
                        </thead>
                        <tbody>
                            @for (var f : empresas)
                                <tr>
                                    <td><strong>${f.empresa().getName()}</strong></td>
                                    <td>${f.empresa().getStartMonth()}</td>
                                    <td>${f.establecimientos()}</td>
                                    <td>${f.usuarios()}</td>
                                    <td>
                                        @if (f.activa())
                                            <span class="estado-activa">Trabajando acá</span>
                                        @else
                                            <form method="post" action="/admin/empresas/${f.empresa().getId()}/entrar">
                                                @if (_csrf != null)
                                                    <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                                                @endif
                                                <button type="submit" class="boton boton-linea">Entrar</button>
                                            </form>
                                        @endif
                                    </td>
                                </tr>
                            @endfor
                        </tbody>
                    </table>
                </div>
            @endif

            <h3>Nueva empresa</h3>
            <form method="post" action="/admin/empresas" class="filtros">
                @if (_csrf != null)
                    <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                @endif
                <label>Nombre<input name="nombre" required></label>
                <label>Mes inicial<input type="month" name="mesInicial" required></label>
                <label>Usuario del administrador<input name="adminUsuario" required></label>
                <label>Contraseña<input type="password" name="adminPassword" required></label>
                <button type="submit" class="boton">Crear empresa</button>
            </form>
            <p class="ayuda">El mes inicial es el mes cuya existencia anterior es el stock inicial. Después, el administrador carga los establecimientos, las categorías y el stock en Configuración.</p>

            <h2>Superadmins</h2>
            <div class="tabla-scroll angosta">
                <table>
                    <thead><tr><th>Usuario</th></tr></thead>
                    <tbody>
                        @for (var u : superadmins)
                            <tr><td>${u.getUsername()}</td></tr>
                        @endfor
                    </tbody>
                </table>
            </div>
            <form method="post" action="/admin/superadmins" class="filtros">
                @if (_csrf != null)
                    <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                @endif
                <label>Usuario<input name="username" required></label>
                <label>Contraseña<input type="password" name="password" required></label>
                <button type="submit" class="boton boton-linea">Agregar superadmin</button>
            </form>
        </main>
    </body>
</html>
