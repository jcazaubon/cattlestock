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
- **Pantallas con JTE y HTMX.** Renderizado en el servidor con templates type-safe; HTMX da la interactividad y se escribe JS propio solo donde HTMX no alcance (por ejemplo, la grilla de carga rápida).
- **Base de datos MariaDB** para producción, con H2 embebida para desarrollo y pruebas.
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

Funciona y está verificado (16 pruebas con `./verify`, o `verify.cmd` en Windows; build completo con `./mvnw test` en verde): el núcleo del dominio en Java puro, sin framework, ya dentro del proyecto Spring Boot (`com.kinetix.cattlestock.domain`).
- Libro de movimientos con stock inicial, los siete tipos de movimiento y carga en bloque (todo o nada).
- Regla de stock negativo, revisada al cierre de cada día, también con movimientos de fecha pasada.
- Planilla mensual por establecimiento con anterior, columnas, final y totales por especie. El final de un mes es el anterior del siguiente.
- Cambios de categoría y traslados (con La Marta) como un solo movimiento de dos lados.
- Cierre de mes en orden, que bloquea la carga en ambos lados de un traslado, y reapertura del último mes.
- Vista general de existencia con filtros por establecimiento, especie y categoría, a una fecha.

Falta: persistencia, Spring Boot, usuarios y roles, pantallas (formulario y grilla), exportar a Excel.

Próximo paso: pasar el núcleo a un proyecto Spring Boot con Maven (en este entorno no hay acceso a Maven Central, así que no se pudo hacer acá), guardar en base de datos y exponer el alta de movimientos y la planilla.
