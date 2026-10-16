@param int indice
@param String tipo
@param String categoria
@param String establecimiento
@param String destinoEstablecimiento
@param String destinoCategoria
@param String cueros
@param java.util.List<com.kinetix.cattlestock.data.CategoryEntity> categorias
@param java.util.List<com.kinetix.cattlestock.data.EstablishmentEntity> establecimientos
@param java.util.List<com.kinetix.cattlestock.data.CategoryEntity> destinoCategorias
@if (tipo != null && tipo.equals("TRASLADO"))
    <label>&rarr; a establecimiento
        <select name="destinoEstablecimiento${indice}"
                hx-get="/alta/campos-de-linea" hx-target="#campos-${indice}"
                hx-include="closest .linea, [name='establecimiento']">
            <option value=""></option>
            @for (var est : establecimientos)
                @if (!est.getCode().equals(establecimiento))
                    <option value="${est.getCode()}" selected="${est.getCode().equals(destinoEstablecimiento)}">${est.getName()}</option>
                @endif
            @endfor
        </select>
    </label>
    <label>&rarr; a categoria
        <select name="destinoCategoria${indice}">
            <option value=""></option>
            @for (var c : destinoCategorias)
                <option value="${c.getCode()}" selected="${c.getCode().equals(destinoCategoria)}">${c.getName()}</option>
            @endfor
        </select>
    </label>
@elseif (tipo != null && tipo.equals("CAMBIO_CATEGORIA"))
    <label>&rarr; a categoria
        <select name="destinoCategoria${indice}">
            <option value=""></option>
            @for (var c : categorias)
                @if (!c.getCode().equals(categoria))
                    <option value="${c.getCode()}" selected="${c.getCode().equals(destinoCategoria)}">${c.getName()}</option>
                @endif
            @endfor
        </select>
    </label>
@elseif (tipo != null && tipo.equals("MORTANDAD"))
    <label class="campo-corto">Cueros
        <input type="number" name="cueros${indice}" min="0" value="${cueros}">
    </label>
@endif
