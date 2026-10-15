@param String tipo
@param String establecimiento
@param String categoria
@param String destinoEstablecimiento
@param String destinoCategoria
@param java.util.List<com.kinetix.cattlestock.data.EstablishmentEntity> establecimientos
@param java.util.List<com.kinetix.cattlestock.data.CategoryEntity> categorias
@param java.util.List<com.kinetix.cattlestock.data.CategoryEntity> destinoCategorias
@if (tipo != null && tipo.equals("TRASLADO"))
    <p>El traslado se asienta por partida doble: sale del origen y entra en el destino, el mismo dia.</p>
    <table border="1">
        <thead>
            <tr>
                <th></th>
                <th>Origen</th>
                <th>Destino</th>
            </tr>
        </thead>
        <tbody>
            <tr>
                <th>Establecimiento</th>
                <td>
                    @for (var e : establecimientos)
                        @if (e.getCode().equals(establecimiento))
                            ${e.getName()}
                        @endif
                    @endfor
                </td>
                <td>
                    <select name="destinoEstablecimiento" required
                            hx-get="/alta/destino-categorias" hx-target="#destino-categorias" hx-include="this">
                        <option value=""></option>
                        @for (var est : establecimientos)
                            @if (!est.getCode().equals(establecimiento))
                                <option value="${est.getCode()}" selected="${est.getCode().equals(destinoEstablecimiento)}">${est.getName()}</option>
                            @endif
                        @endfor
                    </select>
                </td>
            </tr>
            <tr>
                <th>Categoria</th>
                <td>
                    @for (var c : categorias)
                        @if (c.getCode().equals(categoria))
                            ${c.getName()}
                        @endif
                    @endfor
                </td>
                <td>
                    <span id="destino-categorias">
                        @template.fragment.destinoCategorias(destinoCategorias = destinoCategorias, destinoCategoria = destinoCategoria)
                    </span>
                </td>
            </tr>
        </tbody>
    </table>
@elseif (tipo != null && tipo.equals("CAMBIO_CATEGORIA"))
    <p>El cambio de categoria se asienta por partida doble: sale de la categoria origen y entra en la destino, dentro del mismo establecimiento.</p>
    <table border="1">
        <thead>
            <tr>
                <th></th>
                <th>Origen</th>
                <th>Destino</th>
            </tr>
        </thead>
        <tbody>
            <tr>
                <th>Categoria</th>
                <td>
                    @for (var c : categorias)
                        @if (c.getCode().equals(categoria))
                            ${c.getName()}
                        @endif
                    @endfor
                </td>
                <td>
                    <select name="destinoCategoria" required>
                        <option value=""></option>
                        @for (var c : categorias)
                            @if (!c.getCode().equals(categoria))
                                <option value="${c.getCode()}" selected="${c.getCode().equals(destinoCategoria)}">${c.getName()}</option>
                            @endif
                        @endfor
                    </select>
                </td>
            </tr>
        </tbody>
    </table>
@elseif (tipo != null && tipo.equals("MORTANDAD"))
    <p>
        <label>Cueros<br>
            <input type="number" name="cueros" min="0">
        </label>
    </p>
@endif
