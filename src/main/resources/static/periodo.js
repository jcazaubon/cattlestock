// El selector de periodo son dos listas (mes y año); este script mantiene el campo oculto aaaa-mm que se envia.
document.addEventListener("change", function (e) {
  var s = e.target;
  if (!s.matches || !s.matches("select[data-periodo]")) return;
  var nombre = s.getAttribute("data-periodo");
  var mes = document.querySelector('select[data-periodo="' + nombre + '"][data-parte="mes"]');
  var anio = document.querySelector('select[data-periodo="' + nombre + '"][data-parte="anio"]');
  var oculto = document.getElementById("periodo-" + nombre);
  if (!mes || !anio || !oculto) return;
  oculto.value = anio.value + "-" + (mes.value.length < 2 ? "0" : "") + mes.value;
  oculto.dispatchEvent(new Event("change", { bubbles: true }));
});
