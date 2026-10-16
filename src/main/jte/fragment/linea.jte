@param com.kinetix.cattlestock.web.AltaController.LineaForm linea
@param java.util.List<com.kinetix.cattlestock.data.CategoryEntity> categorias
@param java.util.List<com.kinetix.cattlestock.data.EstablishmentEntity> establecimientos
@param String establecimiento
@param java.util.List<com.kinetix.cattlestock.data.CategoryEntity> destinoCategorias
<div class="linea" id="linea-${linea.indice()}">
    <label>Categoria
        <select name="categoria${linea.indice()}">
            <option value=""></option>
            @for (var c : categorias)
                <option value="${c.getCode()}" selected="${c.getCode().equals(linea.categoria())}">${c.getName()}</option>
            @endfor
        </select>
    </label>
    <label>Tipo
        <select name="tipo${linea.indice()}"
                hx-get="/alta/campos-de-linea" hx-target="#campos-${linea.indice()}"
                hx-include="closest .linea, [name='establecimiento']">
            <option value=""></option>
            <option value="COMPRA" selected="${"COMPRA".equals(linea.tipo())}">Compra</option>
            <option value="VENTA" selected="${"VENTA".equals(linea.tipo())}">Venta</option>
            <option value="NACIMIENTO" selected="${"NACIMIENTO".equals(linea.tipo())}">Nacimiento o produccion</option>
            <option value="MORTANDAD" selected="${"MORTANDAD".equals(linea.tipo())}">Mortandad</option>
            <option value="CONSUMO" selected="${"CONSUMO".equals(linea.tipo())}">Consumo</option>
            <option value="TRASLADO" selected="${"TRASLADO".equals(linea.tipo())}">Traslado</option>
            <option value="CAMBIO_CATEGORIA" selected="${"CAMBIO_CATEGORIA".equals(linea.tipo())}">Cambio de categoria</option>
        </select>
    </label>
    <label class="campo-corto">Cantidad
        <input type="number" name="cantidad${linea.indice()}" min="1" value="${linea.cantidad()}">
    </label>
    <div id="campos-${linea.indice()}" class="linea-campos">
        @template.fragment.camposLinea(indice = linea.indice(), tipo = linea.tipo(), categoria = linea.categoria(),
                establecimiento = establecimiento, destinoEstablecimiento = linea.destinoEstablecimiento(),
                destinoCategoria = linea.destinoCategoria(), cueros = linea.cueros(),
                categorias = categorias, establecimientos = establecimientos, destinoCategorias = destinoCategorias)
    </div>
    <button type="button" class="quitar" title="Quitar esta linea" onclick="this.closest('.linea').remove()">Quitar</button>
</div>
