@param java.util.List<com.kinetix.cattlestock.data.CategoryEntity> categoriasEmpresa
@param String editar
@param String movida
@param String error
@param org.springframework.security.web.csrf.CsrfToken _csrf
<div id="tabla-categorias">
@if (error != null)
<p class="error"><strong>${error}</strong></p>
@endif
@if (categoriasEmpresa.isEmpty())
    <p class="vacio">Todavía no hay categorías. Agrega una arriba o importalas.</p>
@else
    <div class="tabla-scroll">
        <table class="listado-categorias">
            <thead>
                <tr><th>Orden</th><th>Categoría</th><th>Código</th><th>Especie</th><th>Estado</th><th></th></tr>
            </thead>
            <tbody>
                @for (var f : categoriasEmpresa)
                    @if (f.getCode().equals(editar))
                        <tr class="elegida">
                            <td></td>
                            <td colspan="4">
                                <form id="renombrar" method="post" action="/configuracion/categorias/${f.getCode()}/nombre" class="edicion-fila">
                                    @if (_csrf != null)
                                        <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                                    @endif
                                    <input name="nombre" value="${f.getName()}" required aria-label="Nombre de la categoría" autofocus>
                                </form>
                            </td>
                            <td class="botones">
                                <button type="submit" form="renombrar" class="boton boton-icono" title="Guardar nombre" aria-label="Guardar nombre">@template.fragment.icono(nombre = "tilde")</button>
                                <form method="get" action="/configuracion">
                                    <input type="hidden" name="seccion" value="categorias">
                                    <button type="submit" class="boton boton-linea boton-icono" title="Cancelar" aria-label="Cancelar">@template.fragment.icono(nombre = "cruz")</button>
                                </form>
                            </td>
                        </tr>
                    @else
                        <tr class="${(f.isEnabled() ? "" : "deshabilitada") + (f.getCode().equals(movida) ? " movida" : "")}">
                            <td class="orden">
                                <form method="post" action="/configuracion/categorias/${f.getCode()}/subir"
                                      hx-post="/configuracion/categorias/${f.getCode()}/subir" hx-target="#tabla-categorias" hx-swap="outerHTML">
                                    @if (_csrf != null)
                                        <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                                    @endif
                                    <button type="submit" id="subir-${f.getCode()}" class="boton boton-linea boton-icono" title="Subir" aria-label="Subir ${f.getName()}"
                                            disabled="${f.getCode().equals(categoriasEmpresa.get(0).getCode())}">@template.fragment.icono(nombre = "subir")</button>
                                </form>
                                <form method="post" action="/configuracion/categorias/${f.getCode()}/bajar"
                                      hx-post="/configuracion/categorias/${f.getCode()}/bajar" hx-target="#tabla-categorias" hx-swap="outerHTML">
                                    @if (_csrf != null)
                                        <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                                    @endif
                                    <button type="submit" id="bajar-${f.getCode()}" class="boton boton-linea boton-icono" title="Bajar" aria-label="Bajar ${f.getName()}"
                                            disabled="${f.getCode().equals(categoriasEmpresa.get(categoriasEmpresa.size() - 1).getCode())}">@template.fragment.icono(nombre = "bajar")</button>
                                </form>
                            </td>
                            <td>${f.getName()}</td>
                            <td class="codigo">${f.getCode()}</td>
                            <td>${f.getSpecies().name().charAt(0) + f.getSpecies().name().substring(1).toLowerCase()}</td>
                            <td class="estado">${f.isEnabled() ? "Habilitada" : "Deshabilitada"}</td>
                            <td class="botones">
                                <form method="get" action="/configuracion">
                                    <input type="hidden" name="seccion" value="categorias">
                                    <input type="hidden" name="editar" value="${f.getCode()}">
                                    <button type="submit" class="boton boton-linea boton-icono" title="Editar el nombre" aria-label="Editar ${f.getName()}">@template.fragment.icono(nombre = "editar")</button>
                                </form>
                                <form method="post" action="/configuracion/categorias/${f.getCode()}/estado">
                                    @if (_csrf != null)
                                        <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                                    @endif
                                    <input type="hidden" name="habilitada" value="${f.isEnabled() ? "false" : "true"}">
                                    <button type="submit" class="boton boton-linea boton-icono" title="${f.isEnabled() ? "Deshabilitar" : "Habilitar"}"
                                            aria-label="${f.isEnabled() ? "Deshabilitar" : "Habilitar"} ${f.getName()}">@template.fragment.icono(nombre = f.isEnabled() ? "ojo-tachado" : "ojo")</button>
                                </form>
                                <form method="post" action="/configuracion/categorias/${f.getCode()}/baja"
                                      data-confirmar="Se elimina de la lista de la empresa y no se puede deshacer."
                                      data-confirmar-titulo="Dar de baja ${f.getName()}" data-confirmar-boton="Dar de baja" data-confirmar-peligro="si">
                                    @if (_csrf != null)
                                        <input type="hidden" name="${_csrf.getParameterName()}" value="${_csrf.getToken()}">
                                    @endif
                                    <button type="submit" class="boton boton-peligro boton-icono" title="Dar de baja" aria-label="Dar de baja ${f.getName()}">@template.fragment.icono(nombre = "papelera")</button>
                                </form>
                            </td>
                        </tr>
                    @endif
                @endfor
            </tbody>
        </table>
    </div>
@endif
</div>
