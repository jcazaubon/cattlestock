@param java.util.List<com.kinetix.cattlestock.data.CategoryEntity> categorias
@param java.util.Map<String, String> valores
@for (var c : categorias)
    <tr>
        <td>${c.getName()}</td>
        <td><input type="number" name="compra_${c.getCode()}" min="0" value="${valores.get("compra_" + c.getCode())}"></td>
        <td><input type="number" name="nacimiento_${c.getCode()}" min="0" value="${valores.get("nacimiento_" + c.getCode())}"></td>
        <td><input type="number" name="venta_${c.getCode()}" min="0" value="${valores.get("venta_" + c.getCode())}"></td>
        <td><input type="number" name="mortandad_${c.getCode()}" min="0" value="${valores.get("mortandad_" + c.getCode())}"></td>
        <td><input type="number" name="cueros_${c.getCode()}" min="0" value="${valores.get("cueros_" + c.getCode())}"></td>
        <td><input type="number" name="consumo_${c.getCode()}" min="0" value="${valores.get("consumo_" + c.getCode())}"></td>
    </tr>
@endfor
@if (categorias.isEmpty())
    <tr><td colspan="7">Elegi un establecimiento.</td></tr>
@endif
