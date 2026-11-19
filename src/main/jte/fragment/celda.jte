@param int valor
@param boolean vacia = false
<td class="${valor == 0 ? "cero" : ""}${vacia ? " col-vacia" : ""}">${valor == 0 ? "-" : String.valueOf(valor)}</td>
