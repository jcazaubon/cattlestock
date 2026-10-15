# Persistencia, alta de movimientos y planilla

Estado: completo (2026-10-15)

## Que cambió

- El dominio (com.kinetix.cattlestock.domain) quedó igual: es el validador y la calculadora. No se duplican reglas fuera de ahí.
- Capa de datos (com.kinetix.cattlestock.data): tablas company, establishment, category, opening, movement y month_close con Spring Data JPA.
- Capa de aplicación (com.kinetix.cattlestock.app.LedgerService): arma un Ledger desde la base consultando por establecimiento (se expande por los traslados hasta cerrar el componente conectado) y delega las reglas en el dominio. El armado rehace los cierres en orden a partir del estado persistido.
- Pantallas JTE + HTMX: alta de movimientos (formulario con selects dependientes) y planilla mensual (con cerrar y reabrir mes). El indice pasó a ser la portada con links.
- Las 16 pruebas del dominio pasaron de main() a JUnit. verify y verify.cmd quedaron como atajos a mvnw test.
- Dev: H2 en archivo ./data/cattlestock con consola en /h2-console. Tests: H2 en memoria. Producción: MariaDB (config comentada).
- Seed de datos de prueba la primera vez (empresa, Principal, La Marta y stocks). Provisional hasta que haya pantallas de configuración.

## Reglas que se respetaron (ya estaban en el dominio, probadas)

- Stock = stock inicial + movimientos. No se guarda.
- Traslado y cambio de categoria son un movimiento con dos lados.
- Mes cerrado no admite cargas, en ningun lado del traslado. Cierres en orden, reapertura solo del ultimo.

## Criterios de aceptacion y resultado

1. El alta de un movimiento lo persiste y aparece en la planilla del mes. Verificado (LedgerServiceTest, WebTest).
2. Un movimiento rechazado no persiste nada. Verificado (venta que deja en negativo, mes cerrado, categoria invalida, stock inicial negativo).
3. Un traslado persistido aparece como salida en el origen y entrada en el destino. Verificado.
4. Cerrar un mes persiste el cierre y bloquea la carga; reabrir el ultimo lo permite. El bloqueo sigue vigente al reconstruir desde la base. Verificado.
5. La planilla se genera por establecimiento y mes con anterior, columnas, final y totales por especie, igual que el dominio. Verificado (los 16 del dominio siguen en verde, mas las de persistencia).
6. La pantalla de alta muestra el error en espanol y conserva lo escrito cuando el dominio rechaza. Verificado (WebTest).
7. mvnw test en verde: 30 pruebas (16 del dominio como JUnit, 7 de persistencia, 1 de contexto, 1 de humo de JTE, 5 de web).

## Aprendizajes del camino

- JTE: los atributos condicionales se escriben como smart attributes (selected="${condicion}"), no como expresiones en el nombre del atributo.
- JTE: los import van antes que los param; los tipos que se escriben en ${...} son String, numeros, Enum y Content (YearMonth pasa por toString()).
- JTE: la llamada a un template en subdirectorio es @template.fragment.nombre(...) y el nombre del archivo debe coincidir exactamente con el nombre llamado (un guion de diferencia hace que hasTemplate devuelva false y el view resolver caiga en un forward circular).
