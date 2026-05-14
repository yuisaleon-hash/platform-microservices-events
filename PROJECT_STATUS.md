# Project Status

Ultima actualizacion: 2026-05-13

## Resumen ejecutivo

El repositorio contiene tres microservicios Spring Boot funcionales a nivel de arranque: `auth-service`, `event-service` y `reservation-service`. Cada servicio compila y tiene una prueba basica de carga de contexto que pasa contra PostgreSQL local.

El sistema ya cubre autenticacion JWT, CRUD de eventos y flujo basico de reservas. Todavia faltan integraciones propias de una plataforma distribuida completa, como coordinacion entre reservas y eventos, descuento de cupos, configuracion externa, contenedores, pruebas de negocio y manejo centralizado de errores.

## Estado por servicio

| Servicio | Estado | Observaciones |
| --- | --- | --- |
| `auth-service` | Implementado base | Registro y login con BCrypt/JWT. Usuarios nuevos se crean como `USER`. |
| `event-service` | Implementado base | CRUD de eventos con autorizacion por rol. Escritura requiere `ADMIN`. |
| `reservation-service` | Implementado base con limitaciones | Reservas por usuario autenticado, pago y cancelacion. Usa precio temporal fijo. |

## Funcionalidades implementadas

- Registro de usuarios.
- Login con emision de JWT.
- Validacion de JWT en servicios protegidos.
- Roles `USER` y `ADMIN` en el token.
- CRUD de eventos.
- Estados de evento: `ACTIVE`, `CANCELLED`, `SOLD_OUT`.
- Creacion de reservas asociadas al email autenticado.
- Consulta de reservas propias.
- Pago de reservas pendientes.
- Cancelacion de reservas pendientes.
- Persistencia con JPA/PostgreSQL por microservicio.

## Verificacion ejecutada

Comandos ejecutados el 2026-05-13:

```powershell
cd auth-service
.\mvnw.cmd test
```

Resultado: `BUILD SUCCESS`, `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`.

```powershell
cd event-service
.\mvnw.cmd test
```

Resultado: `BUILD SUCCESS`, `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`.

```powershell
cd reservation-service
.\mvnw.cmd test
```

Resultado: `BUILD SUCCESS`, `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`.

Condicion de la verificacion: las pruebas levantaron contexto Spring y conectaron con PostgreSQL local usando las bases de datos configuradas en cada `application.properties`.

## Limitaciones actuales

- No existe integracion entre `reservation-service` y `event-service`.
- Al crear una reserva no se valida si el evento existe.
- Al crear una reserva no se valida capacidad disponible.
- Al crear, pagar o cancelar una reserva no se actualiza `availableCapacity` del evento.
- `reservation-service` calcula `totalAmount` con un precio fijo temporal de `100.00`.
- No hay flujo publico para crear usuarios `ADMIN`.
- No hay `docker-compose` para levantar PostgreSQL y servicios juntos.
- No hay API Gateway ni service discovery.
- No hay migraciones de base de datos con Flyway/Liquibase.
- No hay perfiles separados para desarrollo, test y produccion.
- Las credenciales y secreto JWT estan en `application.properties`; deben moverse a variables de entorno o gestor de secretos.
- Las pruebas actuales son principalmente de arranque; no cubren reglas de negocio ni endpoints.
- No hay manejo global de excepciones uniforme.
- No hay documentacion OpenAPI/Swagger.

## Riesgos tecnicos

- La consistencia entre eventos y reservas puede romperse porque los servicios no coordinan cupos ni existencia de eventos.
- El uso de `ddl-auto=update` es practico para desarrollo, pero riesgoso para ambientes controlados.
- El secreto JWT compartido en archivos locales aumenta riesgo de exposicion.
- Las pruebas dependen de PostgreSQL local, lo que puede dificultar CI/CD si no se prepara la base de datos.
- `auth-service` usa Java 21 mientras los otros servicios declaran Java 17; conviene estandarizar la version objetivo.

## Pendientes recomendados

1. Mover credenciales y secreto JWT a variables de entorno.
2. Agregar `docker-compose.yml` para PostgreSQL y los tres servicios.
3. Definir mecanismo para crear o promover usuarios `ADMIN`.
4. Integrar `reservation-service` con `event-service` para validar evento, precio y disponibilidad.
5. Implementar actualizacion atomica o eventual de cupos.
6. Agregar pruebas unitarias de servicios y pruebas de controladores.
7. Agregar perfiles `dev`, `test` y `prod`.
8. Agregar migraciones versionadas de base de datos.
9. Agregar manejo global de errores con respuestas consistentes.
10. Documentar API con OpenAPI/Swagger.
11. Evaluar API Gateway para exponer una entrada unica.

## Estado de repositorio observado

- Hay tres proyectos Maven independientes.
- No hay `pom.xml` padre en la raiz.
- No hay `docker-compose.yml` en la raiz.
- No hay README previo en la raiz.
- Se observo un cambio pendiente en `auth-service` antes de crear esta documentacion. No fue revertido.

## Criterio de listo actual

El proyecto esta listo para ejecucion local controlada si PostgreSQL esta levantado y las tres bases de datos existen. No esta listo como despliegue productivo ni como plataforma distribuida completa hasta resolver configuracion segura, integracion entre servicios, pruebas de negocio y empaquetado de infraestructura.
