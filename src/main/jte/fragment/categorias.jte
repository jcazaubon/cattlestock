@param java.util.List<com.kinetix.cattlestock.data.CategoryEntity> categorias
@param String categoria
<label>Categoria<br>
    <select name="categoria" required>
        <option value=""></option>
        @for (var c : categorias)
            <option value="${c.getCode()}" selected="${c.getCode().equals(categoria)}">${c.getName()}</option>
        @endfor
    </select>
</label>
