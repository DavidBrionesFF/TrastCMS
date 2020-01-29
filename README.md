# TrastCMS
Esta es una herramienta para la gestion de contenido, totalmente escalable y gratuito escrito en Java y Spring Boot y MySQL.

Trats CMS esta orientado en la arquitectura de microservicios.

### Instalacion

Aun se encuentra en etapa de desarrollo.
 1. Primero se debe de cargar la base de datos la cual es squema.sql en una base de datos mysql.
 2. Debemos cargar la base de datos de prueba y de produccion.
 3. Se debe de compilar con mvn clean install, este ejecutara los test.
 4. Ejecutar el jar producido por Spring Boot
 
 ### Caracteristicas
 1. Soporte para JDBC MySQL.
 2. API REST en su primera version.
 3. Panel de Administracion MVC Con Thymeleaft.
 4. Control de Usuarios, Permisos y Grupos.
 5. Manejo de matedatos en los usuarios, para poder agregar campos personalizados.
 6. Manejo de Categorias en las publicaciones.
 
## Esquema de base de datos
Este es el esquema basico de nuestra base de datos el cual te la puedes descargar
![Esquema](https://raw.githubusercontent.com/DavidBrionesFF/TrastCMS/master/TrastCMS%20Model.JPG)
