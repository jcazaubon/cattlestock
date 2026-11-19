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
11. **Dependencias.** Correr un análisis de vulnerabilidades (por ejemplo OWASP Dependency-Check) antes de cada publicación y mantener Spring Boot al día.

## Pendiente a futuro

- Doble factor de autenticación para administradores y superadmin.
- Recuperación de contraseña por correo (hoy la restablece el administrador creando de nuevo el acceso).
- Registro de auditoría de inicios de sesión.
