/*
 * Dialogo de confirmacion propio de la app, en lugar del cuadro del navegador.
 *
 * Un formulario que necesita confirmacion lo pide con atributos:
 *   data-confirmar="texto"           el mensaje (si falta, el formulario se envia sin preguntar)
 *   data-confirmar-titulo="titulo"   opcional
 *   data-confirmar-boton="Eliminar"  el texto del boton de aceptar, opcional
 *   data-confirmar-peligro="si"      pinta el boton de aceptar en rojo, opcional
 *
 * El dialogo (un elemento <dialog>) vive en la barra de cada pantalla. Esc o un clic afuera cancelan, el foco
 * arranca en Cancelar y vuelve al boton que lo abrio. Al aceptar se vuelve a enviar el mismo formulario, asi
 * que tambien funciona con los que envia htmx.
 */
(function () {
    "use strict";
    if (window.__confirmarListo) return;
    window.__confirmarListo = true;

    var pendiente = null;
    var origen = null;

    /** El dialogo actual: htmx puede reemplazar el cuerpo de la pagina, asi que se busca cada vez. */
    function dialogo() {
        var el = document.getElementById("dialogo-confirmar");
        if (!el || typeof el.showModal !== "function") return null;
        if (!el.__enlazado) {
            el.__enlazado = true;
            el.querySelector("[data-rol=cancelar]").addEventListener("click", function () { el.close("cancelar"); });
            el.querySelector("[data-rol=aceptar]").addEventListener("click", function () { el.close("aceptar"); });
            // un clic en el fondo, fuera del cuadro, cancela
            el.addEventListener("click", function (e) { if (e.target === el) el.close("cancelar"); });
            el.addEventListener("close", function () {
                var form = pendiente;
                var boton = origen;
                pendiente = null;
                origen = null;
                if (el.returnValue === "aceptar" && form && form.isConnected) {
                    form.setAttribute("data-confirmado", "si");
                    form.requestSubmit();
                } else if (boton && boton.isConnected) {
                    boton.focus();
                }
            });
        }
        return el;
    }

    document.addEventListener("submit", function (e) {
        var form = e.target;
        if (!(form instanceof HTMLFormElement)) return;
        var mensaje = form.getAttribute("data-confirmar");
        if (!mensaje) return;
        if (form.getAttribute("data-confirmado") === "si") {
            form.removeAttribute("data-confirmado");
            return;
        }
        var el = dialogo();
        if (!el) return;
        // se frena este envio (tambien para htmx) hasta que la persona acepte
        e.preventDefault();
        e.stopPropagation();
        pendiente = form;
        origen = e.submitter || document.activeElement;
        el.querySelector("[data-rol=titulo]").textContent = form.getAttribute("data-confirmar-titulo") || "Confirmar";
        el.querySelector("[data-rol=texto]").textContent = mensaje;
        var aceptar = el.querySelector("[data-rol=aceptar]");
        aceptar.textContent = form.getAttribute("data-confirmar-boton") || "Aceptar";
        aceptar.classList.toggle("boton-peligro-solido", form.getAttribute("data-confirmar-peligro") === "si");
        el.returnValue = "";
        el.showModal();
        el.querySelector("[data-rol=cancelar]").focus();
    }, true);
})();
