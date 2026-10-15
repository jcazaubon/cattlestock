@param String tipo
@param String establecimiento
@param String categoria
@param String destinoEstablecimiento
@param String destinoCategoria
@param java.util.List<com.kinetix.cattlestock.data.EstablishmentEntity> establecimientos
@param java.util.List<com.kinetix.cattlestock.data.CategoryEntity> categorias
@param java.util.List<com.kinetix.cattlestock.data.CategoryEntity> destinoCategorias
@if (tipo != null && tipo.equals("TRASLADO"))
    <p>
        <label>Establecimiento destino<br>
            <select name="destinoEstablecimiento" required
                    hx-get="/alta/destino-categorias" hx-target="#destino-categorias" hx-include="this">
                <option value=""></option>
                @for (var est : establecimientos)
                    @if (!est.getCode().equals(establecimiento))
                        <option value="${est.getCode()}" selected="${est.getCode().equals(destinoEstablecimiento)}">${est.getName()}</option>
                    @endif
                @endfor
            </select>
        </label>
    </p>
    <span id="destino-categorias">
        @template.fragment.destinoCategorias(destinoCategorias = destinoCategorias, destinoCategoria = destinoCategoria)
    </span>
@elseif (tipo != null && tipo.equals("CAMBIO_CATEGORIA"))
    <p>
        <label>Categoria destino<br>
            <select name="destinoCategoria" required>
                <option value=""></option>
                @for (var c : categorias)
                    @if (!c.getCode().equals(categoria))
                        <option value="${c.getCode()}">${c.getName()}</option>
                    @endif
                @endfor
            </select>
        </label>
    </p>
@elseif (tipo != null && tipo.equals("MORTANDAD"))
    <p>
        <label>Cueros<br>
            <input type="number" name="cueros" min="0">
        </label>
    </p>
@endif
