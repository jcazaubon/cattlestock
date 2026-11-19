@param String usuario
@param boolean esAdmin
@param boolean esSuperadmin
@param String empresaNombre
@param java.util.List<com.kinetix.cattlestock.data.CompanyEntity> empresasMenu
@param Long empresaActivaId
@param String ruta
@param org.springframework.security.web.csrf.CsrfToken _csrf
<header class="barra">
    <a class="marca" href="/"><span class="caravana" aria-hidden="true"></span>CattleStock</a>
    <nav hx-boost="true" aria-label="Principal">
        <a href="/" aria-current="${ruta.equals("/") || ruta.startsWith("/planilla") ? "page" : null}">Planillas</a>
        <a href="/grilla" aria-current="${ruta.startsWith("/alta") || ruta.startsWith("/grilla") ? "page" : null}">Cargar movimientos</a>
        <a href="/existencia" aria-current="${ruta.startsWith("/existencia") ? "page" : null}">Existencia</a>
        @if (esAdmin)
            <a href="/historial" aria-current="${ruta.startsWith("/historial") ? "page" : null}">Historial</a>
        @endif
    </nav>
    @if (!empresaNombre.isEmpty())
        @if (esSuperadmin)
            <div class="menu-empresa">
                <button type="button" class="empresa" aria-haspopup="true" aria-expanded="false" title="Cambiar de empresa">${empresaNombre}</button>
                <div class="menu-usuario-opciones">
                    @for (var e : empresasMenu)
                        <form method="post" action="/admin/empresas/${e.getId()}/entrar" hx-boost="false">
                            @if (_csrf != null)
                                <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                            @endif
                            <button type="submit" class="${e.getId().equals(empresaActivaId) ? "activa" : ""}">${e.getName()}</button>
                        </form>
                    @endfor
                    <div class="separador"></div>
                    <a href="/admin">Administrar empresas</a>
                </div>
            </div>
        @else
            <span class="empresa">${empresaNombre}</span>
        @endif
    @endif
    <div class="menu-usuario">
        <button type="button" class="usuario" aria-haspopup="true" aria-expanded="false" aria-label="Menu de ${usuario}">
            <span class="avatar" aria-hidden="true">${usuario.length() > 0 ? usuario.substring(0, 1).toUpperCase() : "C"}</span>
            <span class="nombre-usuario">${usuario}</span>
        </button>
        <div class="menu-usuario-opciones">
            @if (esSuperadmin)
                <a href="/admin">Administración</a>
            @endif
            @if (esAdmin)
                <a href="/configuracion">Configuración</a>
            @endif
            <a href="/cuenta/clave">Cambiar contraseña</a>
            <div class="separador"></div>
            <form method="post" action="/logout" hx-boost="false">
                @if (_csrf != null)
                    <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                @endif
                <button type="submit">Salir</button>
            </form>
        </div>
    </div>
</header>
<dialog id="dialogo-confirmar" class="dialogo" aria-labelledby="dialogo-titulo" aria-describedby="dialogo-texto">
    <div class="dialogo-cuerpo">
        <h2 id="dialogo-titulo" data-rol="titulo">Confirmar</h2>
        <p id="dialogo-texto" data-rol="texto"></p>
    </div>
    <div class="dialogo-acciones">
        <button type="button" class="boton boton-linea" data-rol="cancelar">Cancelar</button>
        <button type="button" class="boton" data-rol="aceptar">Aceptar</button>
    </div>
</dialog>
<script src="/confirmar.js" defer></script>
<script src="/menus.js" defer></script>
<script src="/periodo.js" defer></script>
<script src="/pestanas.js" defer></script>
<script src="/lineas.js" defer></script>
