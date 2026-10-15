# Excel, grilla, existencia, configuracion y usuarios

Estado: completo (2026-10-15)

## Que se hizo

1. **Excel**: POI 5.5.1; `PlanillaExcelExporter` con secciones por especie, columnas de entradas y salidas, anterior, final y totales; descarga desde /planilla y desde el boton "Descargar Excel". Prueba que lee el .xlsx de vuelta.
2. **Grilla**: `LedgerService.addBatch` (todo o nada); pantalla /grilla con una fila por categoria y columnas compra, nacimiento, venta, mortandad, cueros y consumo; sticky values al rechazar; traslados y cambios siguen por el alta.
3. **Vista general**: `LedgerService.existencia` y pantalla /existencia con filtros de establecimiento, especie, categoria y fecha.
4. **Configuracion**: /configuracion con empresa, establecimientos, categorias, stock inicial y usuarios. El seed queda como atajo de desarrollo.
5. **Usuarios y roles**: Spring Security con login propio (JTE), logout, BCrypt; usuarios con rol ADMIN/CARGA y establecimientos asignados; el carga solo ve y carga en los suyos; reabrir mes solo admin; configuracion solo admin. Seed crea admin/admin. JTE necesita el token CSRF en cada formulario: `CsrfControllerAdvice` lo deja en el modelo y cada form lo incluye.

## Verificacion

- `mvnw test`: 46 pruebas en verde (16 dominio, 9 servicio, 1 excel, 1 contexto, 1 humo, 18 web y seguridad).
- Casos negativos: grilla con fila invalida no persiste nada; carga en establecimiento ajeno rechazada; reabrir siendo carga rechaza y la planilla queda cerrada; login con password mal rechaza.
- Los diez criterios del primer resultado util de PRODUCT.md estan hechos.

## Aprendizajes Boot 4 / Security 7

- `@AutoConfigureMockMvc` vive en `org.springframework.boot.webmvc.test.autoconfigure`.
- `CsrfToken` esta en `org.springframework.security.web.csrf`.
- Para que `@WithMockUser` enganche con MockMvc hace falta `spring-boot-security-test` en el classpath de test; sin ese modulo, solo funcionan los post-processors `user()`/`csrf()`.
- JTE: strings dentro de expresiones con comillas dobles (la comilla simple es char); `@param` debe declarar todo lo que el template usa, incluido el `_csrf`.
