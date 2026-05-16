# Project Status

Ultima actualizacion: 2026-05-15

## Resumen ejecutivo

El repositorio contiene tres microservicios Spring Boot funcionales y un API Gateway: `auth-service`, `event-service`, `reservation-service` y `api-gateway`. Cada servicio compila y tiene una prueba basica de carga de contexto.

El sistema ya cubre autenticacion JWT, CRUD de eventos, flujo basico de reservas, comunicacion REST entre reservas y eventos, mensajeria Kafka local y entrada unica mediante Spring Cloud Gateway. Todavia faltan capacidades de plataforma distribuida completa, como configuracion externa, service discovery, pruebas de negocio amplias y manejo centralizado de errores.

## Estado por servicio

| Servicio | Estado | Observaciones |
| --- | --- | --- |
| `auth-service` | Implementado base | Registro y login con BCrypt/JWT. Usuarios nuevos se crean como `USER`. |
| `event-service` | Implementado con integracion | CRUD de eventos con autorizacion por rol. Escritura requiere `ADMIN`. Expone endpoints internos para capacidad. |
| `reservation-service` | Implementado con integracion | Reservas por usuario autenticado, pago y cancelacion. Valida eventos y actualiza capacidad mediante `event-service`. |
| `api-gateway` | Implementado base | Entrada unica en puerto `8085` con routing, validacion JWT centralizada y rate limiting basico. |

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
- API Gateway con rutas hacia `auth-service`, `event-service` y `reservation-service`.
- Validacion JWT centralizada en `api-gateway`.
- Rate limiting basico en `api-gateway`.

## FASE 5 - Comunicacion entre Microservicios

Estado: COMPLETADA

Validaciones realizadas:

- Comunicacion REST entre `reservation-service` y `event-service` funcionando.
- Validacion de eventos y capacidad funcionando.
- `availableCapacity` se actualiza correctamente.
- Kafka funcionando localmente mediante Docker Compose.
- Evento `reservation.created` validado correctamente mediante Kafka consumer.
- JWT, reservas y pagos continuan funcionando.

## FASE 6 - API Gateway

Estado: COMPLETADA BASE

Validaciones realizadas:

- `api-gateway` creado como microservicio Spring Boot/Spring Cloud Gateway.
- Puerto del gateway configurado en `8085`.
- Routing configurado hacia `auth-service`, `event-service` y `reservation-service`.
- Rutas publicas mantenidas para `POST /auth/login` y `POST /auth/register`.
- Validacion JWT centralizada funcionando para rutas protegidas.
- Header `Authorization` se propaga correctamente hacia los microservicios.
- Seguridad interna de `event-service` y `reservation-service` se mantiene como defensa adicional.
- Rate limiting basico funcionando en rutas sensibles.
- Exceso de requests devuelve `429 Too Many Requests`.
- Pruebas de `api-gateway`, `auth-service`, `event-service` y `reservation-service` ejecutadas con `BUILD SUCCESS`.

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

- El API Gateway usa rate limiting en memoria; no es distribuido ni persistente.
- Los microservicios siguen accesibles directamente por sus puertos locales (`8080`, `8081`, `8082`).
- No hay flujo publico para crear usuarios `ADMIN`.
- No hay `docker-compose` para levantar PostgreSQL y servicios juntos.
- No hay service discovery.
- No hay migraciones de base de datos con Flyway/Liquibase.
- No hay perfiles separados para desarrollo, test y produccion.
- Las credenciales y secreto JWT estan en `application.properties`; deben moverse a variables de entorno o gestor de secretos.
- Las pruebas actuales son principalmente de arranque; no cubren reglas de negocio ni endpoints.
- No hay manejo global de excepciones uniforme.
- No hay documentacion OpenAPI/Swagger.

## Riesgos tecnicos

- La consistencia entre eventos y reservas depende de llamadas REST sincronas y compensacion basica; aun no hay patron distribuido robusto.
- El rate limiting en memoria del gateway no sirve para multiples instancias sin un backend compartido.
- El uso de `ddl-auto=update` es practico para desarrollo, pero riesgoso para ambientes controlados.
- El secreto JWT compartido en archivos locales aumenta riesgo de exposicion.
- Las pruebas dependen de PostgreSQL local, lo que puede dificultar CI/CD si no se prepara la base de datos.
- `auth-service` usa Java 21 mientras los otros servicios declaran Java 17; conviene estandarizar la version objetivo.

## Pendientes recomendados

1. Mover credenciales y secreto JWT a variables de entorno.
2. Agregar `docker-compose.yml` para PostgreSQL y los tres servicios.
3. Definir mecanismo para crear o promover usuarios `ADMIN`.
4. Evaluar Redis u otro backend compartido para rate limiting distribuido.
5. Implementar service discovery si se requiere escalar servicios.
6. Agregar pruebas unitarias de servicios y pruebas de controladores.
7. Agregar perfiles `dev`, `test` y `prod`.
8. Agregar migraciones versionadas de base de datos.
9. Agregar manejo global de errores con respuestas consistentes.
10. Documentar API con OpenAPI/Swagger.
11. Restringir acceso directo a microservicios internos mediante red/infraestructura.

## Estado de repositorio observado

- Hay tres proyectos Maven independientes.
- Hay un proyecto Maven independiente adicional para `api-gateway`.
- No hay `pom.xml` padre en la raiz.
- Hay `docker-compose.yml` en la raiz para Kafka y Zookeeper.
- No hay README previo en la raiz.
- Se observo un cambio pendiente en `auth-service` antes de crear esta documentacion. No fue revertido.

## Criterio de listo actual

El proyecto esta listo para ejecucion local controlada si PostgreSQL esta levantado, las tres bases de datos existen, Kafka esta disponible cuando se pruebe mensajeria y los servicios se ejecutan junto con `api-gateway`. No esta listo como despliegue productivo ni como plataforma distribuida completa hasta resolver configuracion segura, acceso directo a servicios internos, pruebas de negocio y empaquetado de infraestructura.
