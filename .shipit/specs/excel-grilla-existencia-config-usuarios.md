# Excel, grilla, existencia, configuracion y usuarios

Estado: en curso (2026-10-15)

Cubre todo el backlog de .shipit/open.md en orden de valor para el producto.

## 1. Exportar la planilla a Excel

- Dependencia Apache POI (poi-ooxml).
- `ExcelExporter` arma un .xlsx a partir de la Planilla del dominio: secciones por especie, columnas de entradas y salidas, anterior, final y totales, con formato parecido a la planilla actual (negritas y anchos razonables).
- Endpoint GET /planilla/excel?establecimiento=&mes= que baja `planilla-P-2026-09.xlsx`.
- Prueba: genera el archivo desde el escenario conocido y lo lee con POI para verificar celdas clave (anterior, columnas, final).

## 2. Carga en grilla

- `LedgerService.addBatch` (todo o nada, ya probado en el dominio con addAll).
- Pantalla /grilla: fecha, establecimiento y una fila por categoria con columnas compra, nacimiento, venta, mortandad, consumo y cueros. Se envian solo las celdas con numero.
- Traslados y cambios de categoria siguen por el formulario de alta (necesitan destino).
- Pruebas: un lote con una fila invalida no persiste nada; un lote valido persiste todo y aparece en la planilla.

## 3. Vista general de existencia

- `LedgerService.existencia` (el dominio ya calcula con filtros).
- Pantalla /existencia con filtros por establecimiento, especie, categoria y fecha. Tabla establecimiento, categoria, cantidad.
- Prueba: la pantalla muestra las cantidades del escenario en la fecha elegida.

## 4. Pantallas de configuracion

- /configuracion: empresa (crear la primera vez, ver siempre), establecimientos (listado y alta), categorias por establecimiento (listado y alta), stock inicial (una fila por categoria).
- El seed queda como atajo de desarrollo; con las pantallas ya se puede configurar de verdad.
- Prueba: se crea un establecimiento, una categoria y el stock inicial por pantalla.

## 5. Usuarios y roles

- spring-boot-starter-security con login propio (JTE), logout, BCrypt.
- UserEntity: usuario, password hasheado, rol (ADMIN/CARGA), establecimientos asignados.
- El carga solo ve y carga en sus establecimientos; el admin ve todo. Reabrir mes queda solo para admin (capa web). Configuracion solo para admin.
- Seed crea admin/admin en desarrollo.
- Pruebas: carga no llega a /configuracion ni puede reabrir; ve solo lo asignado. Admin puede todo.

## Fuera de este lote

- MariaDB real en produccion (config ya comentada).
- Evaluar Laravel 13 con Filament.
- Auditoria de edicion de meses cerrados.

## Como se verifica

- `mvnw test` verde despues de cada entrega, un commit por entrega.
