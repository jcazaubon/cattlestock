# CattleStock

Registro de movimientos de hacienda para empresas ganaderas. Reemplaza las planillas mensuales de Excel (existencia anterior, entradas, salidas, existencia final) por una app donde los movimientos se cargan cuando ocurren, o una vez por semana, y la planilla mensual se genera sola.

Cada establecimiento tiene su propio catálogo de categorías (agrupadas por especie: vacunos, yeguarizos, lanares). Los traslados entre establecimientos y los cambios de categoría se asientan por partida doble: un solo movimiento con dos lados, que aparece como salida en el origen y como entrada en el destino.

## Stack

- **Java 25** + **Spring Boot 4.1.1** (Maven, wrapper incluido)
- **JTE** (templates type-safe) + **HTMX** (interactividad sin JS propio)
- **Spring Data JPA** con **H2** en archivo para desarrollo y **MariaDB** para producción
- **Spring Security** con login propio, roles (administrador / carga) y establecimientos asignados por usuario

## Requisitos

- JDK 25 con `JAVA_HOME` apuntándole (no hace falta Maven: el proyecto trae wrapper)

## Arrancar

```powershell
git clone https://github.com/jcazaubon/cattlestock.git
cd cattlestock
.\mvnw.cmd spring-boot:run      # en Linux/Mac: ./mvnw spring-boot:run
```

Abrir http://localhost:8080 y entrar con **admin / admin**.

La primera vez, el seed crea la empresa de prueba con dos establecimientos (Principal y La Marta), sus categorías, el stock inicial y el usuario admin. La base de desarrollo es H2 en archivo (`./data/cattlestock`) con consola en `/h2-console`. Para producción con MariaDB, descomentar la configuración de `src/main/resources/application.properties`.

## Pruebas

```powershell
.\mvnw.cmd test        # o los atajos ./verify y verify.cmd
```

Más de 50 pruebas: núcleo del dominio (16), servicio con base de datos, exportación a Excel, web y seguridad.

## Estructura

| Paquete | Qué hay |
|---|---|
| `domain` | El núcleo puro, sin framework: libro de movimientos, planilla mensual, reglas (stock negativo, cierre de mes en orden, lotes todo o nada) |
| `data` | Entidades JPA y repositorios (empresa, establecimientos, categorías, stock inicial, movimientos, cierres, usuarios) |
| `app` | Servicios: `LedgerService` arma el libro desde la base y delega las reglas en el dominio; exportación a Excel |
| `web` | Controladores y pantallas JTE (alta por lote, grilla, planilla, existencia, configuración) |
| `security` | Configuración de sesión, roles y permisos |

Las pantallas viven en `src/main/jte` (fragmentos HTMX en `src/main/jte/fragment`).

## Más documentación

- **Wiki del repo**: guía de uso pantalla por pantalla y detalles de arquitectura
- **PRODUCT.md**: definición del producto, decisiones y estado actual

## Licencia

[MIT](LICENSE) — libre de usar, copiar y modificar, conservando el aviso de copyright.

