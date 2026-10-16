@param java.util.List<com.kinetix.cattlestock.web.AltaController.LineaForm> lineas
@param java.util.List<com.kinetix.cattlestock.data.CategoryEntity> categorias
@param java.util.List<com.kinetix.cattlestock.data.EstablishmentEntity> establecimientos
@param String establecimiento
@param java.util.Map<java.lang.Integer, java.util.List<com.kinetix.cattlestock.data.CategoryEntity>> destinoCategoriasPorIndice
@for (var linea : lineas)
    @template.fragment.linea(linea = linea, categorias = categorias, establecimientos = establecimientos,
            establecimiento = establecimiento, destinoCategorias = destinoCategoriasPorIndice.get(linea.indice()))
@endfor
