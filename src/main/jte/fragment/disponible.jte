@param com.kinetix.cattlestock.web.AltaController.Disponible disponible
<details class="disponible">
    <summary><strong>${disponible.titulo()}</strong>: ${disponible.total()} animales al ${disponible.fecha()}</summary>
    @if (disponible.grupos().isEmpty())
        <p class="vacio">Todavía no hay categorías cargadas.</p>
    @endif
    @for (var grupo : disponible.grupos())
        <div class="disponible-grupo">
            @if (disponible.grupos().size() > 1)
                <p class="disponible-especie">${grupo.especie()}</p>
            @endif
            <ul>
                @for (var item : grupo.items())
                    <li class="${item.cantidad() == 0 ? "cero" : ""}"><span>${item.nombre()}</span><strong>${item.cantidad()}</strong></li>
                @endfor
            </ul>
        </div>
    @endfor
</details>
