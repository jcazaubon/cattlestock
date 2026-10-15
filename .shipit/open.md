# Pendiente

- Mover cada metodo de `LedgerTest` a un test de JUnit sin cambiar su contenido, para que las 16 pruebas corran con `./mvnw test` (hoy corren con `./verify` o `verify.cmd`).
- Persistencia: tablas para establecimientos, categorias, stock inicial, movimientos y cierres de mes. El stock se sigue calculando.
- `Ledger` recorre todos los movimientos en memoria. Con base de datos hay que consultar por establecimiento, categoria y rango de fechas.
- La reapertura de un mes debe quedar limitada a administradores en la capa de la aplicacion.
- Mas adelante: evaluar Laravel 13 con Filament para el panel de administracion y las pantallas. No empezar hasta que el nucleo en Spring Boot tenga persistencia y alta de movimientos.
