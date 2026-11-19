/*
 * Los menus de la barra (empresa y usuario) se abren al tocar o con el teclado, no solo al pasar el mouse:
 * en el celular no existe el "pasar por encima". Un clic en el boton abre o cierra su menu, un clic afuera o
 * Esc lo cierran, y aria-expanded avisa el estado a los lectores de pantalla.
 */
(function () {
    "use strict";
    if (window.__menusListos) return;
    window.__menusListos = true;

    function menus() { return document.querySelectorAll(".menu-usuario, .menu-empresa"); }

    function cerrar(excepto) {
        menus().forEach(function (m) {
            if (m === excepto) return;
            m.classList.remove("abierto");
            var b = m.querySelector(":scope > button");
            if (b) b.setAttribute("aria-expanded", "false");
        });
    }

    document.addEventListener("click", function (e) {
        var boton = e.target.closest(".menu-usuario > button, .menu-empresa > button");
        if (!boton) { cerrar(null); return; }
        var menu = boton.parentElement;
        var abrir = !menu.classList.contains("abierto");
        cerrar(menu);
        menu.classList.toggle("abierto", abrir);
        boton.setAttribute("aria-expanded", abrir ? "true" : "false");
    });

    document.addEventListener("keydown", function (e) {
        if (e.key !== "Escape") return;
        var abierto = document.querySelector(".menu-usuario.abierto, .menu-empresa.abierto");
        if (!abierto) return;
        cerrar(null);
        var b = abierto.querySelector(":scope > button");
        if (b) b.focus();
    });
})();
