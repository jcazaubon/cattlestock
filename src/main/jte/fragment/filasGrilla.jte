@param java.util.List<com.kinetix.cattlestock.data.CategoryEntity> categorias
@param java.util.Map<String, String> valores = java.util.Collections.emptyMap()
@param String especie = ""
@for (var c : categorias)
    <tr hidden="${!especie.isEmpty() && !c.getSpecies().name().equals(especie)}">
        <td>${c.getName()}</td>
        <td><input type="number" name="compra_${c.getCode()}" min="0" value="${valores.get("compra_" + c.getCode())}"></td>
        <td><input type="number" name="nacimiento_${c.getCode()}" min="0" value="${valores.get("nacimiento_" + c.getCode())}"></td>
        <td><input type="number" name="venta_${c.getCode()}" min="0" value="${valores.get("venta_" + c.getCode())}"></td>
        <td><input type="number" name="mortandad_${c.getCode()}" min="0" value="${valores.get("mortandad_" + c.getCode())}"></td>
        <td><input type="number" name="cueros_${c.getCode()}" min="0" value="${valores.get("cueros_" + c.getCode())}"></td>
        <td><input type="number" name="consumo_${c.getCode()}" min="0" value="${valores.get("consumo_" + c.getCode())}"></td>
        <td><input type="number" name="pastoreo_${c.getCode()}" min="0" value="${valores.get("pastoreo_" + c.getCode())}"></td>
        <td><input type="number" name="recuento_${c.getCode()}" min="0" value="${valores.get("recuento_" + c.getCode())}"></td>
    </tr>
@endfor
@if (categorias.isEmpty())
    <tr><td colspan="9">Elegí un establecimiento.</td></tr>
@elseif (!especie.isEmpty() && categorias.stream().noneMatch(c -> c.getSpecies().name().equals(especie)))
    <tr><td colspan="9">La empresa no tiene categorías de esa especie.</td></tr>
@endif
