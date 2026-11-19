@param String establecimiento
@param String mes
@param String cat
@param String tipo
@param boolean eliminados
<input type="hidden" name="establecimiento" value="${establecimiento}">
<input type="hidden" name="mes" value="${mes}">
<input type="hidden" name="cat" value="${cat}">
<input type="hidden" name="tipo" value="${tipo}">
@if (eliminados)
    <input type="hidden" name="eliminados" value="1">
@endif
