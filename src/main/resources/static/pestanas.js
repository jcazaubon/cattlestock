// Al cambiar de forma de carga se conserva lo del encabezado: período, fecha, establecimiento y nota.
document.addEventListener("click", function (e) {
  var a = e.target.closest && e.target.closest("[data-pestanas-carga] a");
  if (!a) return;
  var form = document.querySelector("main form[action='/grilla'], main form[action='/movimientos']");
  if (!form) return;
  var url = new URL(a.getAttribute("href"), location.origin);
  ["periodo", "fecha", "establecimiento", "nota"].forEach(function (n) {
    var c = form.elements[n];
    if (c && c.value) url.searchParams.set(n, c.value);
  });
  a.href = url.pathname + url.search;
});
