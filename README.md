# Platform Microservices Events

Proyecto backend de gestion de eventos y reservas construido con microservicios Spring Boot. El sistema esta separado en servicios independientes para autenticacion, administracion de eventos y reservas.

## Arquitectura

| Servicio | Puerto | Responsabilidad | Base de datos |
| --- | ---: | --- | --- |
| `auth-service` | `8080` | Registro, login y emision de JWT | `auth_db` |
| `event-service` | `8081` | CRUD de eventos | `event_service_db` |
| `reservation-service` | `8082` | Creacion, consulta, pago y cancelacion de reservas | `reservation_service_db` |

Cada servicio tiene su propio proyecto Maven, su propio `application.properties` y su propia base de datos PostgreSQL.

## Stack

- Java 17 para `event-service` y `reservation-service`
- Java 21 para `auth-service`
- Spring Boot 4.0.6
- Spring Web MVC
- Spring Security
- Spring Data JPA
- PostgreSQL
- JWT con `jjwt 0.12.6`
- Lombok
- Maven Wrapper por servicio

## Estructura

```text
.
+-- auth-service/
|   +-- src/main/java/com/events/authservice/
|   +-- src/main/resources/application.properties
+-- event-service/
|   +-- src/main/java/com/events/eventservice/
|   +-- src/main/resources/application.properties
+-- reservation-service/
|   +-- src/main/java/com/events/reservationservice/
|   +-- src/main/resources/application.properties
+-- README.md
+-- PROJECT_STATUS.md
```

## Requisitos

- JDK 21 instalado. Aunque dos servicios compilan con `release 17`, usar JDK 21 permite ejecutar tambien `auth-service`.
- PostgreSQL local.
- Bases de datos creadas:

```sql
CREATE DATABASE auth_db;
CREATE DATABASE event_service_db;
CREATE DATABASE reservation_service_db;
```

Revisa las credenciales de cada archivo `src/main/resources/application.properties` antes de ejecutar. Actualmente estan configuradas para PostgreSQL local.

## Ejecutar los servicios

Abre una terminal por servicio.

```powershell
cd auth-service
.\mvnw.cmd spring-boot:run
```

```powershell
cd event-service
.\mvnw.cmd spring-boot:run
```

```powershell
cd reservation-service
.\mvnw.cmd spring-boot:run
```

## Probar

Desde la raiz del repositorio:

```powershell
cd auth-service
.\mvnw.cmd test
```

```powershell
cd event-service
.\mvnw.cmd test
```

```powershell
cd reservation-service
.\mvnw.cmd test
```

## Autenticacion

`auth-service` expone:

| Metodo | Endpoint | Auth | Descripcion |
| --- | --- | --- | --- |
| `POST` | `/auth/register` | No | Registra usuario y devuelve JWT |
| `POST` | `/auth/login` | No | Autentica usuario y devuelve JWT |
| `GET` | `/auth/me` | Si | Devuelve el email autenticado |
| `GET` | `/auth/ping` | Si | Verifica respuesta del servicio |

Registro:

```http
POST http://localhost:8080/auth/register
Content-Type: application/json

{
  "username": "usuario1",
  "email": "usuario1@example.com",
  "password": "password123"
}
```

Login:

```http
POST http://localhost:8080/auth/login
Content-Type: application/json

{
  "email": "usuario1@example.com",
  "password": "password123"
}
```

La respuesta incluye un `token`. Para consumir `event-service` y `reservation-service`, envia:

```http
Authorization: Bearer <token>
```

Los usuarios registrados por el endpoint publico se crean con rol `USER`. El enum tambien soporta `ADMIN`, pero no existe un endpoint publico para crear administradores.

## Eventos

`event-service` expone:

| Metodo | Endpoint | Rol requerido | Descripcion |
| --- | --- | --- | --- |
| `GET` | `/events` | `USER` o `ADMIN` | Lista eventos |
| `GET` | `/events/{id}` | `USER` o `ADMIN` | Obtiene un evento |
| `POST` | `/events` | `ADMIN` | Crea un evento |
| `PUT` | `/events/{id}` | `ADMIN` | Actualiza un evento |
| `DELETE` | `/events/{id}` | `ADMIN` | Elimina un evento |

Payload de creacion/actualizacion:

```json
{
  "title": "Conferencia de Arquitectura",
  "description": "Evento tecnico sobre microservicios",
  "location": "Lima",
  "eventDate": "2026-06-15T19:00:00",
  "price": 120.00,
  "totalCapacity": 100,
  "availableCapacity": 100,
  "status": "ACTIVE"
}
```

Estados soportados: `ACTIVE`, `CANCELLED`, `SOLD_OUT`.

## Reservas

`reservation-service` expone:

| Metodo | Endpoint | Rol requerido | Descripcion |
| --- | --- | --- | --- |
| `POST` | `/reservations` | `USER` o `ADMIN` | Crea una reserva para el usuario autenticado |
| `GET` | `/reservations/me` | `USER` o `ADMIN` | Lista reservas del usuario autenticado |
| `GET` | `/reservations/{id}` | `USER` o `ADMIN` | Obtiene una reserva propia |
| `POST` | `/reservations/{id}/pay` | `USER` o `ADMIN` | Marca una reserva como pagada |
| `POST` | `/reservations/{id}/cancel` | `USER` o `ADMIN` | Cancela una reserva pendiente |

Crear reserva:

```json
{
  "eventId": 1,
  "quantity": 2
}
```

Pagar reserva:

```json
{
  "paymentMethod": "CARD"
}
```

Estados soportados: `PENDING`, `PAID`, `CANCELLED`.

## Notas de implementacion

- Los servicios validan JWT con el mismo secreto configurado en sus `application.properties`.
- `event-service` protege escritura de eventos para `ROLE_ADMIN`.
- `reservation-service` solo permite consultar, pagar o cancelar reservas del usuario autenticado.
- `reservation-service` aun usa un precio unitario temporal de `100.00`; no consulta el precio real del evento.
- No hay `docker-compose`, API Gateway, descubrimiento de servicios ni integracion HTTP entre microservicios en el estado actual.

## Documentacion adicional

Consulta [PROJECT_STATUS.md](./PROJECT_STATUS.md) para ver el estado actual, pruebas ejecutadas, limitaciones y pendientes recomendados.
