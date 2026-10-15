@param String establecimiento
@param String categoria
@param String tipo
@param String destinoEstablecimiento
@param String destinoCategoria
@param java.util.List<com.kinetix.cattlestock.data.EstablishmentEntity> establecimientos
@param java.util.List<com.kinetix.cattlestock.data.CategoryEntity> categorias
@param java.util.List<com.kinetix.cattlestock.data.CategoryEntity> destinoCategorias
<p>
    @template.fragment.categorias(categorias = categorias, categoria = categoria)
</p>
<p>
    <label>Tipo<br>
        <select name="tipo" required
                hx-get="/alta/campos" hx-target="#campos"
                hx-include="[name='establecimiento'], [name='categoria'], [name='destinoEstablecimiento'], this">
            <option value=""></option>
            <option value="COMPRA" selected="${"COMPRA".equals(tipo)}">Compra</option>
            <option value="VENTA" selected="${"VENTA".equals(tipo)}">Venta</option>
            <option value="NACIMIENTO" selected="${"NACIMIENTO".equals(tipo)}">Nacimiento o produccion</option>
            <option value="MORTANDAD" selected="${"MORTANDAD".equals(tipo)}">Mortandad</option>
            <option value="CONSUMO" selected="${"CONSUMO".equals(tipo)}">Consumo</option>
            <option value="TRASLADO" selected="${"TRASLADO".equals(tipo)}">Traslado</option>
            <option value="CAMBIO_CATEGORIA" selected="${"CAMBIO_CATEGORIA".equals(tipo)}">Cambio de categoria</option>
        </select>
    </label>
</p>
<div id="campos">
    @template.fragment.campos(tipo = tipo, establecimiento = establecimiento, categoria = categoria,
            destinoEstablecimiento = destinoEstablecimiento, destinoCategoria = destinoCategoria,
            establecimientos = establecimientos, categorias = categorias, destinoCategorias = destinoCategorias)
</div>
