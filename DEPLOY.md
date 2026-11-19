# Notas para el deploy

El deploy está pospuesto. Esta lista reúne lo que hay que resolver **antes de publicar** el sistema con datos reales.
Lo marcado como hecho ya está en el código; el resto son tareas del deploy.

## Hecho en el código

- [x] CSRF activo en todos los formularios; contraseñas con BCrypt.
- [x] Cada empresa solo ve y modifica lo suyo (controlado en cada pantalla y probado en `SeguridadDatosTest` y `MultiempresaTest`).
- [x] Límite de intentos de login: 5 fallidos por usuario o 20 por dirección en 15 minutos bloquean el ingreso durante ese lapso.
- [x] Política de contraseñas: mínimo 8 caracteres, máximo 72, distinta del usuario.
- [x] Las claves que da el administrador son temporales: el usuario debe cambiarlas en su primer ingreso (`/cuenta/clave`).
- [x] Política de contenido (CSP) estricta: sin JavaScript en línea ni de otros orígenes; `Referrer-Policy` y `frame-ancestors`.
- [x] Cookie de sesión `HttpOnly` y `SameSite=Lax`.
- [x] Solo se abren como Excel los archivos que lo son (se comprueba la firma, no solo el nombre).
- [x] Consola H2 solo para el superadmin, y únicamente existe con el perfil `local`.

## Tareas antes de publicar

1. **Clave del superadmin inicial.** Definir `CATTLESTOCK_ADMIN_PASSWORD` antes del primer arranque. Con la de por defecto
   (`admin`) el sistema deja un aviso en el log. Si la base ya existe, cambiar la clave desde el menú de usuario.
2. **Credenciales de la base.** Hoy son `cattlestock` / `cattlestock` (y `root` / `root` en Docker). En el servidor usar
   variables de entorno: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`. No dejarlas
   en `application.properties`. La base no debe ser alcanzable desde afuera (`docker-compose.yml` ya publica el puerto solo en 127.0.0.1).
3. **HTTPS.** Publicar solo detrás de HTTPS y agregar `server.servlet.session.cookie.secure=true`. Spring agrega HSTS solo cuando el pedido llega por HTTPS.
4. **Detrás de un proxy** (nginx, Hostinger, etc.): agregar `server.forward-headers-strategy=native`. Sin eso, el límite de intentos ve la
   dirección del proxy y cuenta a todos los usuarios como uno solo.
5. **Plantillas.** `gg.jte.developmentMode=true` recompila las plantillas en cada pedido. Para producción: precompilarlas
   (`gg.jte.usePrecompiledTemplates=true` con el plugin de Maven de JTE) y apagar el modo desarrollo.
6. **Esquema de la base.** `spring.jpa.hibernate.ddl-auto=update` sirve para desarrollar. En producción pasar a `validate` y manejar los
   cambios con migraciones (Flyway o Liquibase), con copia de seguridad antes de cada cambio.
7. **Copias de seguridad.** Programar el respaldo de la base (`mysqldump` diario) y probar la restauración al menos una vez.
8. **Más de un servidor.** El límite de intentos y las sesiones viven en memoria: con más de una instancia hay que llevarlos
   a una base compartida (por ejemplo, Spring Session con la base de datos).
9. **Registros (logs).** No registran contraseñas ni datos personales; revisar que el servidor tampoco los guarde en el proxy.
10. **Subida de Excel.** Tope de 25 MB y solo administradores. Si se abre a más usuarios, bajar el tope y evaluar un escaneo del archivo.
11. **Dependencias.** Antes de cada publicación: actualizar al último parche de Spring Boot 4.1.x (arrastra Tomcat, Spring Security, Hibernate y Jackson) y
    correr un análisis de vulnerabilidades (por ejemplo OWASP Dependency-Check). La última revisión (19/11/2026) dejó Spring Boot 4.1.3 con Tomcat 11.0.27, que cierra los
    CVE de Tomcat publicados hasta ese día; la versión 4.1.1 traía Tomcat 11.0.24, con varias fallas conocidas. Los avisos de Tomcat están en
    <https://tomcat.apache.org/security-11.html>.

## Oracle Cloud: VM Ampere A1 (ARM), plan Always Free

Plan elegido para la primera prueba. Una sola máquina corre la aplicación, MariaDB y el proxy con HTTPS.
Los números de capacidad del plan hay que confirmarlos en la consola de Oracle, porque cambian.

**Máquina.** Forma `VM.Standard.A1.Flex`, Ubuntu para ARM (aarch64). Con 1 núcleo y 6 GB alcanza de sobra; no hace falta pedir más.
Si la región no tiene capacidad, probar otro dominio de disponibilidad o reintentar más tarde.

**Memoria.** Con 6 GB sobra margen. Valores de partida:

- Aplicación: `-Xms256m -Xmx768m` (subir solo si la observación bajo carga lo pide).
- MariaDB: `innodb_buffer_pool_size=256M`.
- Base: `spring.datasource.hikari.maximum-pool-size=5`.

**Java.** Usar un JDK 25 para aarch64 (Temurin u otro) y comprobar con `java -version` que sea de ARM. El `.jar` es el mismo que se arma en
Windows (Java es portable): se genera con `./mvnw.cmd -DskipTests package` y se copia a la VM. Antes hay que resolver la tarea 5 (plantillas precompiladas).

**Pasos.**

1. Crear la VM y una clave SSH. En la red virtual (VCN), lista de seguridad: abrir 80 y 443; **no** abrir 8080 ni 3306.
2. Abrir también el firewall de la propia máquina (`iptables` en las imágenes de Oracle): puertos 80 y 443. Es el olvido más común.
3. Instalar MariaDB con `apt`, que escuche solo en `127.0.0.1`, y crear una base y un usuario propios (con clave larga y aleatoria, no `cattlestock`).
4. Crear el usuario del sistema `cattlestock` y la carpeta `/opt/cattlestock` con el `.jar`.
5. Guardar los secretos en `/etc/cattlestock.env` (permisos `600`, dueño `root`):

   ```
   SPRING_DATASOURCE_URL=jdbc:mariadb://127.0.0.1:3306/cattlestock
   SPRING_DATASOURCE_USERNAME=cattlestock
   SPRING_DATASOURCE_PASSWORD=<clave larga>
   CATTLESTOCK_ADMIN_PASSWORD=<clave del superadmin>
   SERVER_ADDRESS=127.0.0.1
   SERVER_PORT=8080
   SERVER_SERVLET_SESSION_COOKIE_SECURE=true
   SERVER_FORWARD_HEADERS_STRATEGY=native
   ```

6. Servicio `systemd` (`/etc/systemd/system/cattlestock.service`), para que arranque solo y se reinicie si cae:

   ```
   [Unit]
   Description=CattleStock
   After=network.target mariadb.service

   [Service]
   User=cattlestock
   EnvironmentFile=/etc/cattlestock.env
   ExecStart=/usr/bin/java -Xms256m -Xmx768m -jar /opt/cattlestock/cattlestock.jar
   Restart=on-failure

   [Install]
   WantedBy=multi-user.target
   ```

7. Proxy con HTTPS: Caddy (certificado de Let's Encrypt automático) que reenvía a `127.0.0.1:8080`. Hace falta un nombre de dominio apuntando
   a la IP pública de la VM. Sin dominio no hay certificado válido: para probar se puede entrar por un túnel SSH.
8. Respaldo: `mysqldump` diario con `cron`, comprimido, copiado fuera de la VM (Object Storage de Oracle u otro lado). Probar una restauración.
9. Primer ingreso: entrar como superadmin, cambiar la clave si quedó la inicial, crear la empresa y su administrador (que deberá cambiar su clave temporal).

**Cuidados propios de Oracle.**

- Oracle puede reclamar las instancias gratuitas que pasan mucho tiempo casi inactivas: revisar la política vigente y no depender de la VM sin respaldos.
- Las claves y la clave SSH se guardan fuera del repositorio; nada de esto se commitea.
- Con una sola VM no hay alta disponibilidad: si cae, el sistema queda caído hasta que se reinicie.

## Pendiente a futuro

- Doble factor de autenticación para administradores y superadmin.
- Recuperación de contraseña por correo (hoy la restablece el administrador creando de nuevo el acceso).
- Registro de auditoría de inicios de sesión.
