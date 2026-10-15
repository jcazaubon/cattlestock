@param String error
@param com.kinetix.cattlestock.data.CompanyEntity empresa
@param java.util.List<com.kinetix.cattlestock.data.EstablishmentEntity> establecimientos
@param String seleccionado
@param java.util.List<com.kinetix.cattlestock.web.ConfiguracionController.FilaCategoria> filas
@param java.util.List<com.kinetix.cattlestock.data.UserEntity> usuarios
@param org.springframework.security.web.csrf.CsrfToken _csrf
<!DOCTYPE html>
<html lang="es">
    <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>Configuracion - CattleStock</title>
        <script src="/htmx.min.js"></script>
    </head>
    <body>
        <h1>Configuracion</h1>
        <p><a href="/">Inicio</a></p>
        @if (error != null)
            <p class="error"><strong>${error}</strong></p>
        @endif

        <h2>Empresa</h2>
        @if (empresa == null)
            <form method="post" action="/configuracion/empresa">
                @if (_csrf != null)
                    <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                @endif
                <p>
                    <label>Nombre<br><input name="nombre" required></label>
                </p>
                <p>
                    <label>Mes inicial (el mes cuya existencia anterior es el stock inicial)<br>
                        <input type="month" name="mesInicial" required>
                    </label>
                </p>
                <p><button type="submit">Guardar empresa</button></p>
            </form>
        @else
            <p><strong>${empresa.getName()}</strong>, arranca en ${empresa.getStartMonth()}.</p>
        @endif

        <h2>Establecimientos</h2>
        <ul>
            @for (var est : establecimientos)
                <li><a href="/configuracion?establecimiento=${est.getCode()}">${est.getName()}</a> (${est.getCode()})</li>
            @endfor
        </ul>
        <form method="post" action="/configuracion/establecimientos">
            @if (_csrf != null)
                <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
            @endif
            <p>
                <label>Codigo<br><input name="codigo" required></label>
                <label>Nombre<br><input name="nombre" required></label>
            </p>
            <p><button type="submit">Agregar establecimiento</button></p>
        </form>

        @if (!seleccionado.isEmpty())
            <h2>Categorias y stock inicial</h2>
            <form method="post" action="/configuracion/stock">
                @if (_csrf != null)
                    <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                @endif
                <input type="hidden" name="establecimiento" value="${seleccionado}">
                <table border="1">
                    <thead>
                        <tr>
                            <th>Categoria</th>
                            <th>Especie</th>
                            <th>Stock inicial</th>
                        </tr>
                    </thead>
                    <tbody>
                        @for (var f : filas)
                            <tr>
                                <td>${f.categoria().getName()} (${f.categoria().getCode()})</td>
                                <td>${f.categoria().getSpecies()}</td>
                                <td><input type="number" name="stock_${f.categoria().getCode()}" min="0"
                                        value="${f.cantidad() >= 0 ? String.valueOf(f.cantidad()) : ""}"></td>
                            </tr>
                        @endfor
                    </tbody>
                </table>
                <p><button type="submit">Guardar stock inicial</button></p>
            </form>

            <h3>Nueva categoria</h3>
            <form method="post" action="/configuracion/categorias">
                @if (_csrf != null)
                    <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                @endif
                <input type="hidden" name="establecimiento" value="${seleccionado}">
                <p>
                    <label>Codigo<br><input name="codigo" required></label>
                    <label>Nombre<br><input name="nombre" required></label>
                    <label>Especie<br>
                        <select name="especie">
                            <option value="VACUNOS">Vacunos</option>
                            <option value="YEGUARIZOS">Yeguarizos</option>
                            <option value="LANARES">Lanares</option>
                        </select>
                    </label>
                </p>
                <p><button type="submit">Agregar categoria</button></p>
            </form>
        @endif

        <h2>Usuarios</h2>
        <table border="1">
            <thead>
                <tr>
                    <th>Usuario</th>
                    <th>Rol</th>
                    <th>Establecimientos</th>
                </tr>
            </thead>
            <tbody>
                @for (var u : usuarios)
                    <tr>
                        <td>${u.getUsername()}</td>
                        <td>${u.getRole()}</td>
                        <td>
                            @for (var est : u.getEstablishments())
                                ${est.getName()} (${est.getCode()})
                            @endfor
                        </td>
                    </tr>
                @endfor
            </tbody>
        </table>
        <form method="post" action="/configuracion/usuarios">
            @if (_csrf != null)
                <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
            @endif
            <p>
                <label>Usuario<br><input name="username" required></label>
                <label>Password<br><input type="password" name="password" required></label>
                <label>Rol<br>
                    <select name="rol">
                        <option value="CARGA">Carga</option>
                        <option value="ADMIN">Administrador</option>
                    </select>
                </label>
            </p>
            <p>Establecimientos asignados (el administrador ve todos):<br>
                @for (var est : establecimientos)
                    <label><input type="checkbox" name="establecimientos" value="${est.getCode()}"> ${est.getName()}</label>
                @endfor
            </p>
            <p><button type="submit">Crear usuario</button></p>
        </form>
    </body>
</html>
