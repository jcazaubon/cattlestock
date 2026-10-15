@param java.util.List<com.kinetix.cattlestock.data.CategoryEntity> destinoCategorias
@param String destinoCategoria
<label>Categoria destino<br>
    <select name="destinoCategoria" required>
        <option value=""></option>
        @for (var c : destinoCategorias)
            <option value="${c.getCode()}" selected="${c.getCode().equals(destinoCategoria)}">${c.getName()}</option>
        @endfor
    </select>
</label>
