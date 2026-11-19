# CattleStock

## Propósito

Registro de movimientos de hacienda para empresas ganaderas. Reemplaza las planillas de Excel mensuales (existencia anterior, entradas, salidas, existencia final) por una app donde los movimientos se cargan cuando ocurren, o una vez por semana, y la planilla mensual se genera sola.

Sirve a empresas con varios establecimientos, donde cada establecimiento tiene sus propias categorías de animales. Una "planilla" es el formulario mensual de movimientos de un establecimiento.

## Primer resultado útil

Una persona puede cargar el stock inicial de un establecimiento, registrar movimientos durante el mes y bajar la planilla mensual en Excel con un formato parecido al que usan hoy.

Criterios de aceptación:

1. Se crea una empresa con establecimientos y un catálogo de categorías de la empresa (por ejemplo "Vacas Plantel", "Terneros orejanos"), agrupadas por especie (vacunos, yeguarizos, lanares). Todos los establecimientos de la empresa usan ese catálogo.
2. Se carga el stock inicial por categoría.
3. Se registran movimientos con fecha, categoría, cantidad y nota. Tipos: compra, venta, nacimiento o producción, mortandad (con campo para cuero), consumo, pastoreo, traslado entre establecimientos, cambio de categoría y diferencia de recuento.
4. Un cambio de categoría es un solo movimiento con origen y destino. Un traslado es un solo movimiento con establecimiento de origen y de destino, y siempre es entre establecimientos de la misma empresa (por ejemplo, hacia y desde La Marta). El mismo traslado aparece como salida en uno y como entrada en el otro.
5. Ninguna salida puede dejar una categoría en negativo. La app avisa y no guarda.
6. La carga rápida permite elegir una fecha y completar varias categorías en una grilla, para quien carga por semana o a fin de mes.
7. La planilla mensual se genera por establecimiento y mes: existencia anterior, columnas de entrada, columnas de salida, existencia final y totales. La final de un mes es la anterior del siguiente.
8. Se exporta a Excel con ese formato.
9. Vista general de existencia por empresa, con filtros por establecimiento, mes, categoría, especie y fecha.
10. Los usuarios inician sesión, tienen establecimientos asignados y un rol. Solo ven y cargan en sus establecimientos.

## Roles

La instalación tiene varias empresas independientes: cada una con sus establecimientos, categorías, usuarios y planillas, sin mezclarse con las demás.

- **Superadmin:** está un escalón arriba. Ve todas las empresas, las da de alta junto con su primer administrador y trabaja en la que elija. No pertenece a ninguna empresa.
- **Administrador:** administra solo su empresa. Configura establecimientos, categorías, stock inicial y usuarios, asigna roles y ve todo lo de su empresa. Es el único que puede reabrir un mes y el único que ve, corrige y elimina movimientos ya cargados (Historial).
- **Carga:** registra movimientos y ve la existencia de los establecimientos que tiene asignados.

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
- **Categorías por empresa.** El catálogo es de la empresa y lo comparten todos sus establecimientos: cada uno tiene todas las categorías, con su propio stock inicial y sus movimientos. Cambió respecto de la primera versión (cada establecimiento definía las suyas), porque en las planillas reales la misma categoría se repite entre establecimientos de una empresa. Cada establecimiento muestra todas las categorías de la empresa; si molestan las que no usa, se puede ocultar las que no tienen animales en Existencia.
- **Carga en grilla además del formulario.** Hoy muchos completan todo a fin de mes, aunque lo ideal es cargar a medida que ocurre o por semana. La grilla sirve a los dos hábitos.
- **Cerrar una planilla bloquea la carga de movimientos de ese mes.** Solo un administrador puede reabrirla. Es un supuesto simple para la primera versión.

- **Multiempresa.** Cada establecimiento y cada usuario pertenecen a una empresa, y el Ledger se arma por empresa: un traslado hacia otra empresa falla porque ese establecimiento no existe en su Ledger. El código del establecimiento sigue siendo único en toda la instalación; el panel lo genera a partir del nombre si no se escribe uno. Las categorías nuevas se agregan en Configuración, no al cargar un movimiento.
- **Historial de movimientos (solo administradores).** Lista los movimientos de un mes, con filtros por establecimiento, categoría y tipo, y permite corregirlos y eliminarlos. Eliminar es lógico: la fila queda en la base con quién y cuándo la eliminó, no cuenta en el stock ni en las planillas, se ve marcando Mostrar eliminados y se puede restaurar. Corregir cambia la fecha, la categoría (y la de destino en cambios y traslados), la cantidad, los cueros y la nota; el tipo, el establecimiento y el establecimiento de destino no cambian (para eso se elimina y se vuelve a cargar). Cada cambio queda con quién y cuándo lo hizo. Todo cambio, borrado o restauración se aplica y se reconstruye el libro de la empresa: si algo posterior deja de cerrar (por ejemplo un saldo negativo en una venta más adelante), se revierte y se explica por qué. Un movimiento de un mes cerrado no se toca hasta reabrir la planilla. El administrador solo toca movimientos de los establecimientos en los que opera y de su empresa.
- **Período y fecha de carga.** El período es un mes y un año. En Cargar movimientos (una sola entrada del menú, con las pestañas Por planilla y Por línea, que conservan período, fecha, establecimiento y nota al cambiar) hay un campo Período y la fecha arranca en el último día hábil de ese mes (de lunes a viernes: los feriados no se calculan, porque varían por país, provincia y año). La fecha siempre se puede cambiar a mano; al cambiar el período se vuelve a calcular y se actualizan los animales disponibles a esa fecha. Si ya hay una fecha escrita, por ejemplo al volver de un error, se respeta y su mes pasa a ser el período.
- **Excel de la planilla.** Tiene el aspecto de la app: título con el mes en español, encabezados agrupados (Entradas en verde, Salidas en rojo, Final resaltado), bandas suaves entre filas, números con separador de miles y el cero como guion, filas de total con regla gruesa y un total general propio. Los totales de entradas y salidas también se completan. Sale lista para imprimir en horizontal a un ancho de página, con numeración y la columna de categorías fija. Los valores y la posición de las columnas no cambian.
- **Orden de las categorías.** El administrador sube o baja cada categoría con botones en la lista de Configuración. El orden es de la empresa y vale en la planilla, la existencia, las cargas y la planilla de stock inicial. Con htmx se mueve sin recargar la página: el servidor devuelve solo la tabla, la fila movida se resalta un momento y las flechas llevan un id para que el foco las siga a su lugar nuevo; sin htmx, el formulario funciona como cualquier otro y vuelve a la página.
- **Botones de solo ícono en las categorías.** Las acciones de la lista (subir, bajar, editar el nombre, deshabilitar o habilitar, dar de baja, y guardar o cancelar la edición) son íconos SVG monocromos de 16 px, sin texto. Cada uno lleva `title` (tooltip) y `aria-label` con el nombre de la categoría, para que se entienda y sea accesible. El ícono de estado cambia con la acción: ojo tachado para deshabilitar, ojo para habilitar.
- **Período de la planilla y PDF.** La planilla elige el período con dos listas, mes y año, y arranca en el mes actual. Los enlaces viejos con `mes=aaaa-mm` siguen valiendo, y un mes o año inválido cae al actual. "Descargar PDF" arma la planilla en A4 horizontal con una tabla por especie, entradas en verde, salidas en rojo y la existencia final resaltada (OpenPDF); respeta los mismos permisos que el Excel.
- **Planilla legible (crítica de diseño).** Los ceros de movimiento se muestran como un guion apagado, una banda rotulada agrupa las columnas en Entradas, Salidas y Totales, los encabezados llevan `scope` para lectores de pantalla, cada especie es un `<h2>` bajo el `<h1>` y una leyenda explica Transf. A/DE, Cambio A/DE y Dif. recuento. La tarjeta no tiene scroll propio. "Cerrar mes" queda separado de las descargas y el diálogo nombra el establecimiento. En el celular (≤720 px) la navegación pasa a una barra inferior con destinos de 44 px, los menús se abren al tocar, y la planilla oculta las categorías y columnas sin movimiento, con un interruptor para verlas todas.
- **Cargas con valores por defecto.** El establecimiento arranca elegido (el único que tiene la persona o el último en el que cargó en la sesión), la fecha es hoy en el mes en curso y el último día hábil en los demás, y el panel de disponibles viene cerrado. Las pantallas y mensajes llevan tildes y los mismos nombres en el menú y en el título.
- **Stock inicial vacío no es cero.** Las categorías son de la empresa, pero cada establecimiento usa solo las que tienen stock inicial cargado: un 0 la incluye (el establecimiento la tiene, sin animales) y un campo vacío la deja afuera, sin aparecer en su planilla, su existencia ni sus cargas. Una categoría que ya tuvo movimientos en el establecimiento sigue incluida, para no esconder animales, y un establecimiento sin ningún stock inicial cargado usa todas las de la empresa. La pantalla de stock inicial y la planilla de Excel siguen listando todas las categorías, para poder incluir una cargando un 0; la importación no crea nada con una celda vacía.
- **Diálogos de la app, no del navegador.** Las confirmaciones (dar de baja una categoría, eliminar un movimiento, cerrar un mes) usan un único diálogo propio, un elemento `<dialog>` con los estilos de la app que vive en la barra de cada pantalla. Un formulario lo pide con atributos `data-confirmar` (más título, texto del botón y si es una acción de peligro, que pinta el botón en rojo), y `confirmar.js` frena el envío, abre el diálogo y vuelve a enviar el mismo formulario al aceptar, también con los que envía htmx. Esc, el fondo y Cancelar cancelan, y el foco arranca en Cancelar. Una prueba revisa todas las pantallas y los scripts propios y falla si aparece `confirm`, `alert`, `prompt`, `hx-confirm` u `onsubmit`.
- **Acciones sobre las categorías.** En la lista de Configuración cada fila tiene tres botones: Editar (cambia solo el nombre, en la misma fila; el código no cambia porque lo usan el stock y los movimientos), Deshabilitar o Habilitar, y Baja. Una categoría deshabilitada no se ofrece en Cargar movimiento ni en la carga rápida y el servidor rechaza movimientos sobre ella; sigue en el catálogo y se ve en la planilla y en Existencia si tiene animales, y sale de ahí si está en cero. La baja solo se permite si la categoría nunca se usó (sin stock inicial distinto de cero ni movimientos, ni como origen, destino de un cambio de categoría o de un traslado): si no, el mensaje propone deshabilitarla. Todo queda dentro de la empresa.
- **Importar categorías.** Desde Configuración se suben una o más planillas de hacienda en Excel (.xlsx o .xls), un archivo de texto o categorías pegadas una por línea. No se asume un formato: se busca la columna de categorías por su encabezado ("Categoría", "Viene del mes anterior", "Existencia anterior"...), se prueban las hojas de la última a la primera y se ignoran totales, notas y rótulos de movimiento. Probado con las 19 planillas reales de `I:planillas de hacienda`: acierta en las de formato mensual (de 9 a 48 categorías por archivo) y no encuentra nada en las transpuestas ni en la de pesadas, donde queda el texto pegado. Lo detectado es una propuesta: se revisa nombre y especie, se destilda lo que sobra, y recién ahí se crea (todo o nada). Importar dos veces el mismo archivo no duplica.
- **Stock inicial por Excel.** En Configuración, pestaña Stock inicial, se descarga una planilla ya armada: una fila por categoría de la empresa y una columna por establecimiento (el encabezado lleva el código entre paréntesis para no confundir nombres parecidos), con el stock que ya esté cargado. Se completa con los números y se sube. Una celda vacía no cambia nada y un 0 deja el stock en cero. Antes de aplicar se ve una vista previa (hoy contra nuevo) y todo lo ignorado se avisa: columnas de establecimientos que no existen, categorías desconocidas, negativos, decimales y textos. Se aplica todo o nada contra un solo Ledger, con las mismas reglas que la carga manual: no deja saldos negativos con los movimientos del mes ni toca un mes inicial cerrado. Nunca cruza de empresa.
- **Enums como texto.** Los roles, tipos de movimiento y especies se guardan como VARCHAR y no como ENUM de la base: Hibernate no amplía un ENUM existente, y una base vieja rechazaba los valores nuevos. `EsquemaEnums` convierte al arrancar las columnas que una versión anterior creó como ENUM.

## Opciones para una etapa posterior

- **Laravel 13 con Filament** (panel de administración, pantallas y roles). Queda como alternativa a evaluar más adelante, por ejemplo para el panel de administración o si armar las pantallas en Spring Boot resulta lento. Laravel 13 requiere PHP 8.3 o superior. No se trabaja en esto ahora.

## No ahora

- Edición de meses cerrados con auditoría.
- Trámites oficiales (guías, SENASA).
- Importar los movimientos de las planillas de Excel existentes. Lo que sí se importa: las categorías y el stock inicial (abajo).
- App móvil o uso sin conexión.

## Estado actual

Funciona y está verificado (191 pruebas con `./mvnw test`, atajos `./verify` y `verify.cmd`). Los diez criterios del primer resultado útil están hechos.

- Dominio puro (`com.kinetix.cattlestock.domain`): libro de movimientos con los diez tipos, stock negativo revisado por día, planilla mensual por establecimiento con totales por especie, traslados y cambios de categoría de dos lados, cierres en orden.
- Persistencia JPA (`data`): empresa, establecimientos, categorías, stock inicial, movimientos, cierres y usuarios. El stock se calcula, no se guarda.
- Capa de aplicación (`app`): `LedgerService` arma el Ledger desde la base y delega las reglas en el dominio; `UserService` con BCrypt; seed de datos de prueba y usuario admin/admin.
- Pantallas JTE + HTMX (`web`): alta de movimientos, carga rápida en grilla (todo o nada, con filtro por especie que oculta las filas de las otras sin perder lo escrito), planilla mensual con cerrar y reabrir, vista general de existencia con filtros, configuración completa (empresa, establecimientos, categorías, stock inicial, usuarios) y exportación a Excel con POI.
- Seguridad y multiempresa: login propio y tres roles (superadmin, administrador, carga). El panel `/admin` es solo del superadmin: empresas (con su primer administrador, en una sola operación) y otros superadmins. `/configuracion` es del administrador de su empresa: establecimientos, categorías, stock inicial y usuarios con rol y establecimientos asignables; nunca toca datos de otra empresa. El superadmin elige en qué empresa trabaja haciendo clic en el nombre de la empresa en la barra, y queda en la sesión. Configuración se organiza en pestañas (Establecimientos, Categorías, Usuarios) con tablas limpias y un panel de detalle para editar un establecimiento y su stock inicial; se rediseñó siguiendo la skill no-slop-ui. Las altas (establecimiento, categoría, usuario) van en una sola línea, hay un listado de las categorías de toda la empresa y se puede editar el nombre de un establecimiento (el código no cambia: es la clave de todo lo que lo referencia). El código de establecimientos y categorías es opcional y se genera a partir del nombre. `MigracionEmpresas` pasó la empresa única anterior a "Prueba" y al antiguo administrador a superadmin.
- Pastoreo y recuento (agregados tras revisar planillas reales de 19 archivos): el pastoreo es una salida con columna propia; el recuento se escribe en la grilla como cantidad contada y el sistema asienta la diferencia contra la existencia esperada como "sobran" o "faltan" (columnas Dif. recuento + y -).
- Existencia: con un establecimiento elegido se ve su detalle; con "Todos" es un resumen global sumado por especie y categoría, sin separar por establecimiento. Cada fila trae la existencia al inicio del mes, el movimiento del mes hasta la fecha (compra, nacimiento, venta, mortandad, consumo, pastoreo, cambio de categoría, traslado, recuento), la existencia a la fecha y el porcentaje del total. Solo se muestran las columnas en las que hubo movimiento. La categoría es un combo por especie que se acota al establecimiento.
- Alta de movimientos (cada lote lleva todas las líneas que se quieran, con el botón Agregar otro movimiento, y entran todas o ninguna): un panel "Disponible" lista todos los animales con su cantidad (los del establecimiento elegido o, sin elegir, los de todos) y se actualiza al cambiar el establecimiento o la fecha.
- Aspecto: interfaz de trabajo sobria, revisada con la skill no-slop-ui en todo el sitio. Barra superior clara con la pagina actual subrayada, superficies blancas con borde de 1px sobre un fondo neutro, un solo acento amarillo (el de la caravana) para la accion principal y lo que importa, verde para lo que entra y rojo para lo que sale. Sin degradados, sin movimiento decorativo y con una sola sombra suave. El cebrado verde de las tablas es solo de la planilla, que es la tabla densa. Los tokens estan en `src/main/resources/static/app.css`.

Falta: desplegar en un servidor. La app ya corre sobre MariaDB (Docker en desarrollo con `docker compose up -d`, o H2 en archivo con el perfil `local`).

Próximo paso: desplegarlo en un servidor y cargar la planilla real.

## Lo que muestran las planillas reales

Se revisaron 19 archivos de planillas de hacienda de varias empresas (`I:\planillas de hacienda`). Los formatos que aparecen:

- **Mensual con SALIDA / ENTRADA** (La Mulita, San Pedro, Encarnación, San Ramón, El Cerro, Santa Rosa, La Loma; casi todas las hojas): Mortandad, Consumo, Venta, Pastoreo, Traslado A, Cambio de categoría; Procedencia, Producción, Compra, Cambio de categoría; "Pasa al mes siguiente". Es el formato que imita la app.
- **Mensual con compras, nacidos y transferencias A/De** (Tisa, Tambo y Oriental): igual idea, con mortandad dividida en cueros encontrados y perdidos, y lluvia en mm.
- **Con diferencia de recuento** (PG, Siete Árboles): el conteo físico ajusta el stock. Cubierto con los movimientos de recuento.
- **Hojas partidas a mitad de mes** ("1 al 15", "RECUENTO 16", "17 al 31"): se resuelve con un movimiento de recuento fechado, no con planillas parciales.
- **Transpuestas y con varios rodeos** (El Albardo, María Eugenia): categorías en columnas y rodeos lado a lado. No se imita.
- **Pesadas de camiones** (Hacienda Capitalizada): no es una planilla de stock.

Pendiente, por baja frecuencia: lluvia en mm por mes (3 de 19 archivos) y los kilos por cabeza de las pesadas.
