@param java.util.Map<com.kinetix.cattlestock.domain.Species, java.util.List<com.kinetix.cattlestock.data.CategoryEntity>> grupos
@param String categoria
<option value="">Todas</option>
@for (var grupo : grupos.entrySet())
    <optgroup label="${grupo.getKey().name().charAt(0) + grupo.getKey().name().substring(1).toLowerCase()}">
        @for (var c : grupo.getValue())
            <option value="${c.getCode()}" selected="${c.getCode().equals(categoria)}">${c.getName()}</option>
        @endfor
    </optgroup>
@endfor
