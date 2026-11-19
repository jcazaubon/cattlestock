@import com.kinetix.cattlestock.web.PeriodoVista
@param String nombre
@param String valor
@param boolean actualizaFecha = false
!{var ym = PeriodoVista.analizar(valor);}
<label>Mes
    <select data-periodo="${nombre}" data-parte="mes" aria-label="Mes del período">
        @for (int i = 1; i <= 12; i++)
            <option value="${i}" selected="${i == ym.getMonthValue()}">${PeriodoVista.nombreMes(i)}</option>
        @endfor
    </select>
</label>
<label>Año
    <select data-periodo="${nombre}" data-parte="anio" aria-label="Año del período">
        @for (int a : PeriodoVista.anios(ym))
            <option value="${a}" selected="${a == ym.getYear()}">${a}</option>
        @endfor
    </select>
</label>
@if (actualizaFecha)
    <input type="hidden" id="periodo-${nombre}" name="${nombre}" value="${ym.toString()}"
           hx-get="/campo-fecha" hx-target="#fecha" hx-swap="outerHTML" hx-trigger="change" hx-include="this">
@else
    <input type="hidden" id="periodo-${nombre}" name="${nombre}" value="${ym.toString()}">
@endif
