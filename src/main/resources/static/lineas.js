// Quitar una linea del lote (sin JavaScript en linea, para poder usar una politica de contenido estricta).
document.addEventListener("click", function (e) {
  var b = e.target.closest && e.target.closest("button.quitar");
  if (!b) return;
  var linea = b.closest(".linea");
  if (linea) linea.remove();
});
