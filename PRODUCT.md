# CattleStock

## Propósito

Registro de movimientos de hacienda para empresas ganaderas. Reemplaza las planillas de Excel mensuales (existencia anterior, entradas, salidas, existencia final) por una app donde los movimientos se cargan cuando ocurren, o una vez por semana, y la planilla mensual se genera sola.

Sirve a empresas con varios establecimientos, donde cada establecimiento tiene sus propias categorías de animales. Una "planilla" es el formulario mensual de movimientos de un establecimiento.

## Primer resultado útil

Una persona puede cargar el stock inicial de un establecimiento, registrar movimientos durante el mes y bajar la planilla mensual en Excel con un formato parecido al que usan hoy.

Criterios de aceptación:

1. Se crea una empresa con establecimientos, y cada establecimiento tiene su catálogo de categorías (por ejemplo "Vacas Plantel", "Terneros orejanos"), agrupadas por especie (vacunos, yeguarizos, lanares).
2. Se carga el stock inicial por categoría.
3. Se registran movimientos con fecha, categoría, cantidad y nota. Tipos: compra, venta, nacimiento o producción, mortandad (con campo para cuero), consumo, traslado entre establecimientos y cambio de categoría.
4. Un cambio de categoría es un solo movimiento con origen y destino. Un traslado es un solo movimiento con establecimiento de origen y de destino, y siempre es entre establecimientos de la misma empresa (por ejemplo, hacia y desde La Marta). El mismo traslado aparece como salida en uno y como entrada en el otro.
5. Ninguna salida puede dejar una categoría en negativo. La app avisa y no guarda.
6. La carga rápida permite elegir una fecha y completar varias categorías en una grilla, para quien carga por semana o a fin de mes.
7. La planilla mensual se genera por establecimiento y mes: existencia anterior, columnas de entrada, columnas de salida, existencia final y totales. La final de un mes es la anterior del siguiente.
8. Se exporta a Excel con ese formato.
9. Vista general de existencia por empresa, con filtros por establecimiento, mes, categoría, especie y fecha.
10. Los usuarios inician sesión, tienen establecimientos asignados y un rol. Solo ven y cargan en sus establecimientos.

## Roles

- **Administrador:** configura empresa, establecimientos, categorías y usuarios. Ve todo.
- **Carga:** registra movimientos y ve la existencia de sus establecimientos.

## Decisiones

- **El stock se calcula, no se guarda.** Es el stock inicial más los movimientos. Da historial y evita números que se desarman.
- **La planilla es el formulario mensual de un establecimiento, y se genera a partir de los movimientos.** Nadie escribe celdas de totales a mano. Cada planilla tiene un mes y un estado (abierta o cerrada).
- **Una planilla por establecimiento y por mes.** Incluye secciones por especie (vacunos, yeguarizos, lanares) dentro de la misma planilla.
- **Stack: Java y Spring Boot.** Decidido tras comparar con Laravel: es el stack que mejor domina quien lo construye, y el núcleo ya está escrito y probado en Java. El costo de servidor (2 a 4 GB de RAM) no cambia la decisión.
- **La capa de datos no duplica reglas.** `LedgerService` arma un `Ledger` desde la base (la configuración completa más los movimientos del componente conectado de establecimientos, expandido por traslados) y el dominio valida y calcula. Los cierres se rehacen en orden a partir del estado persistido.
- **Pantallas con JTE y HTMX.** Renderizado en el servidor con templates type-safe; HTMX da la interactividad y se escribe JS propio solo donde HTMX no alcance (por ejemplo, la grilla de carga rápida).
- **Base de datos MariaDB.** Por defecto en Docker (`docker compose up -d`, puerto 3307 para no chocar con un MySQL local); perfil `local` para H2 en archivo; pruebas en H2 en memoria.
- **El cierre de una planilla fija la existencia final, que pasa a ser la anterior del mes siguiente.**
- **"LM" es La Marta, otro establecimiento de la misma empresa.** Sus entradas y salidas son traslados, no compras ni ventas.
- **La primera versión imita el formato de las planillas actuales.** Los usuarios ya lo conocen. Si la app no se les parece, siguen con Excel.
- **Categorías por establecimiento.** Cada establecimiento define las suyas, porque difieren entre sí.
- **Carga en grilla además del formulario.** Hoy muchos completan todo a fin de mes, aunque lo ideal es cargar a medida que ocurre o por semana. La grilla sirve a los dos hábitos.
- **Cerrar una planilla bloquea la carga de movimientos de ese mes.** Solo un administrador puede reabrirla. Es un supuesto simple para la primera versión.

## Opciones para una etapa posterior

- **Laravel 13 con Filament** (panel de administración, pantallas y roles). Queda como alternativa a evaluar más adelante, por ejemplo para el panel de administración o si armar las pantallas en Spring Boot resulta lento. Laravel 13 requiere PHP 8.3 o superior. No se trabaja en esto ahora.

## No ahora

- Varias empresas independientes en la misma instalación, mientras no haya más de un cliente.
- Edición de meses cerrados con auditoría.
- Trámites oficiales (guías, SENASA).
- Importar planillas de Excel existentes. El stock inicial se carga a mano.
- App móvil o uso sin conexión.

## Estado actual

Funciona y está verificado (46 pruebas con `./mvnw test`, atajos `./verify` y `verify.cmd`). Los diez criterios del primer resultado útil están hechos.

- Dominio puro (`com.kinetix.cattlestock.domain`): libro de movimientos con los siete tipos, stock negativo revisado por día, planilla mensual por establecimiento con totales por especie, traslados y cambios de categoría de dos lados, cierres en orden.
- Persistencia JPA (`data`): empresa, establecimientos, categorías, stock inicial, movimientos, cierres y usuarios. El stock se calcula, no se guarda.
- Capa de aplicación (`app`): `LedgerService` arma el Ledger desde la base y delega las reglas en el dominio; `UserService` con BCrypt; seed de datos de prueba y usuario admin/admin.
- Pantallas JTE + HTMX (`web`): alta de movimientos, carga rápida en grilla (todo o nada), planilla mensual con cerrar y reabrir, vista general de existencia con filtros, configuración completa (empresa, establecimientos, categorías, stock inicial, usuarios) y exportación a Excel con POI.
- Seguridad: login propio, todo autenticado, configuración solo para administradores; el usuario de carga solo ve y carga en sus establecimientos; la reapertura de mes queda solo para admin.

Falta: desplegar en un servidor. La app ya corre sobre MariaDB (Docker en desarrollo con `docker compose up -d`, o H2 en archivo con el perfil `local`).

Próximo paso: desplegarlo en un servidor y cargar la planilla real.
