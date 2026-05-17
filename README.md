# Platform Microservices Events

Backend de gestion de eventos y reservas construido con microservicios Spring Boot. La plataforma esta dockerizada con entrada unica por API Gateway, persistencia PostgreSQL separada por servicio y mensajeria Kafka para eventos asincronos.

## Arquitectura

```text
Cliente
  |
  v
api-gateway:8085
  |-- REST --> auth-service:8080
  |-- REST --> event-service:8081
  |-- REST --> reservation-service:8082
                    |
                    |-- REST interno --> event-service:8081

auth-service          --> auth_db
event-service         --> event_service_db
reservation-service   --> reservation_service_db

auth-service          -- Kafka: user.registered --> reservation-service
reservation-service   -- Kafka: reservation.created --> event-service
reservation-service   -- Kafka: payment.completed --> event-service
```

## Microservicios

| Servicio | Puerto interno | Puerto publico | Responsabilidad | Base de datos |
| --- | ---: | ---: | --- | --- |
| `api-gateway` | `8085` | `8085` | Entrada unica, routing, JWT y rate limiting | No aplica |
| `auth-service` | `8080` | No expuesto | Registro, login y emision de JWT | `auth_db` |
| `event-service` | `8081` | No expuesto | CRUD de eventos y capacidad | `event_service_db` |
| `reservation-service` | `8082` | No expuesto | Reservas, pagos y cancelaciones | `reservation_service_db` |

Solo `api-gateway` expone puerto al host. Los demas servicios se comunican por la red interna Docker.

## Stack

- Java 21 para `auth-service`
- Java 17 para `event-service`, `reservation-service` y `api-gateway`
- Spring Boot 4.0.6
- Spring Security
- Spring Data JPA
- Spring Cloud Gateway
- PostgreSQL 16
- Kafka + Zookeeper, imagen Confluent 7.6.1
- JWT con `jjwt 0.12.6`
- Docker Compose

## Estructura

```text
.
|-- api-gateway/
|   |-- Dockerfile
|   |-- .dockerignore
|   `-- src/main/resources/application.properties
|-- auth-service/
|   |-- Dockerfile
|   |-- .dockerignore
|   `-- src/main/resources/application.properties
|-- event-service/
|   |-- Dockerfile
|   |-- .dockerignore
|   `-- src/main/resources/application.properties
|-- reservation-service/
|   |-- Dockerfile
|   |-- .dockerignore
|   `-- src/main/resources/application.properties
|-- docker/
|   `-- postgres/init-multiple-databases.sh
|-- docker-compose.yml
|-- PROJECT_STATUS.md
`-- README.md
```

## Arquitectura Desacoplada

- Cada microservicio es un proyecto Maven independiente.
- No hay dependencias Maven entre microservicios.
- No se comparten entidades JPA.
- No se comparten repositories.
- Cada servicio mantiene su propio `application.properties`.
- Cada servicio tiene su propia base de datos PostgreSQL.
- La comunicacion sincrona entre servicios se realiza por REST.
- La comunicacion asincrona se realiza por Kafka.
- La configuracion sensible y de infraestructura se externaliza mediante variables de entorno.
- Cada servicio puede construir su propia imagen Docker de forma independiente.

## Comunicacion REST

El cliente debe usar siempre el gateway:

```text
http://localhost:8085
```

Rutas principales:

| Ruta gateway | Servicio destino |
| --- | --- |
| `/auth/**` | `auth-service:8080` |
| `/events/**` | `event-service:8081` |
| `/reservations/**` | `reservation-service:8082` |

`reservation-service` usa REST interno hacia:

```text
http://event-service:8081
```

## Comunicacion Kafka

Kafka se usa con hostname interno:

```text
kafka:29092
```

Topics actuales:

| Topic | Productor | Consumidor | Uso |
| --- | --- | --- | --- |
| `user.registered` | `auth-service` | `reservation-service` | Notificacion de usuario registrado |
| `reservation.created` | `reservation-service` | `event-service` | Reserva creada |
| `payment.completed` | `reservation-service` | `event-service` | Pago completado |

## PostgreSQL

PostgreSQL corre como contenedor interno:

```text
postgres:5432
```

Bases de datos:

| Servicio | Base de datos |
| --- | --- |
| `auth-service` | `auth_db` |
| `event-service` | `event_service_db` |
| `reservation-service` | `reservation_service_db` |

El script `docker/postgres/init-multiple-databases.sh` crea las bases al inicializar un volumen PostgreSQL nuevo.

## Infraestructura Docker

Contenedores:

| Contenedor | Servicio |
| --- | --- |
| `platform-postgres` | PostgreSQL |
| `platform-zookeeper` | Zookeeper |
| `platform-kafka` | Kafka |
| `platform-auth-service` | Auth |
| `platform-event-service` | Events |
| `platform-reservation-service` | Reservations |
| `platform-api-gateway` | Gateway |

Red:

```text
platform-network
```

Volumenes:

```text
platform-postgres-data
platform-kafka-data
platform-zookeeper-data
platform-zookeeper-log
```

Healthchecks:

| Servicio | Healthcheck |
| --- | --- |
| PostgreSQL | `pg_isready` |
| Zookeeper | `echo srvr | nc localhost 2181` |
| Kafka | `kafka-broker-api-versions --bootstrap-server kafka:29092` |
| Microservicios | `GET /health` |
| API Gateway | `GET /health` |

## Variables De Entorno

Variables principales:

| Variable | Default local | Uso |
| --- | --- | --- |
| `API_GATEWAY_PORT` | `8085` | Puerto publico del gateway |
| `POSTGRES_USER` | `postgres` | Usuario PostgreSQL |
| `POSTGRES_PASSWORD` | `postgres` | Password PostgreSQL en Docker |
| `POSTGRES_DB` | `postgres` | Base inicial del contenedor |
| `POSTGRES_MULTIPLE_DATABASES` | `auth_db,event_service_db,reservation_service_db` | Bases a crear |
| `AUTH_DB_NAME` | `auth_db` | DB de auth |
| `EVENT_DB_NAME` | `event_service_db` | DB de events |
| `RESERVATION_DB_NAME` | `reservation_service_db` | DB de reservations |
| `JWT_SECRET` | valor de desarrollo | Secreto JWT compartido |
| `JWT_EXPIRATION` | `86400000` | Expiracion JWT en ms |
| `RATE_LIMIT_AUTH_CAPACITY` | `5` | Limite auth |
| `RATE_LIMIT_AUTH_REFILL_PERIOD_SECONDS` | `60` | Ventana auth |
| `RATE_LIMIT_API_CAPACITY` | `30` | Limite API |
| `RATE_LIMIT_API_REFILL_PERIOD_SECONDS` | `60` | Ventana API |

Variables internas usadas por Compose:

```text
POSTGRES_HOST=postgres
POSTGRES_PORT=5432
KAFKA_BOOTSTRAP_SERVERS=kafka:29092
AUTH_SERVICE_URL=http://auth-service:8080
EVENT_SERVICE_URL=http://event-service:8081
RESERVATION_SERVICE_URL=http://reservation-service:8082
APP_KAFKA_ENABLED=true
```

## Ejecutar Con Docker

Desde la raiz:

```powershell
docker compose up --build -d
```

Ver estado:

```powershell
docker compose ps
```

Ver logs:

```powershell
docker compose logs -f api-gateway auth-service event-service reservation-service
```

Detener:

```powershell
docker compose down
```

Reiniciar desde cero, borrando volumenes:

```powershell
docker compose down -v
docker compose up --build -d
```

## Comandos De Validacion

Health del gateway:

```powershell
Invoke-RestMethod http://localhost:8085/health
```

Health interno de servicios:

```powershell
docker exec platform-auth-service wget -q -O - http://localhost:8080/health
docker exec platform-event-service wget -q -O - http://localhost:8081/health
docker exec platform-reservation-service wget -q -O - http://localhost:8082/health
```

PostgreSQL:

```powershell
docker exec platform-postgres psql -U postgres -d auth_db -c "\dt"
docker exec platform-postgres psql -U postgres -d event_service_db -c "\dt"
docker exec platform-postgres psql -U postgres -d reservation_service_db -c "\dt"
```

Kafka:

```powershell
docker exec platform-kafka kafka-topics --bootstrap-server kafka:29092 --list
docker exec platform-kafka kafka-broker-api-versions --bootstrap-server kafka:29092
```

Puertos publicos:

```powershell
docker compose ps
```

Debe aparecer publicado solo `api-gateway` en `8085`.

## Pruebas Funcionales Basicas

Registro:

```powershell
curl -X POST http://localhost:8085/auth/register -H "Content-Type: application/json" -d "{\"username\":\"usuario1\",\"email\":\"usuario1@example.com\",\"password\":\"password123\"}"
```

Login:

```powershell
curl -X POST http://localhost:8085/auth/login -H "Content-Type: application/json" -d "{\"email\":\"usuario1@example.com\",\"password\":\"password123\"}"
```

Usar el token:

```text
Authorization: Bearer <token>
```

Crear evento requiere rol `ADMIN`. Los usuarios creados por `/auth/register` son `USER`; para pruebas locales puede actualizarse el rol directamente en `auth_db`.

Crear evento:

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

## Checklist Final De Validacion

- [ ] `docker compose up --build -d`
- [ ] Todos los contenedores en `running`
- [ ] Healthchecks `healthy`
- [ ] Solo `api-gateway` expone puerto publico
- [ ] Registro via gateway
- [ ] Login via gateway
- [ ] JWT requerido en rutas protegidas
- [ ] CRUD de eventos via gateway
- [ ] Creacion de reservas via gateway
- [ ] Pago de reservas via gateway
- [ ] Cancelacion de reservas via gateway
- [ ] Actualizacion de capacidad de eventos
- [ ] Topics Kafka creados
- [ ] Producers Kafka publican eventos
- [ ] Consumers Kafka consumen eventos
- [ ] Tablas creadas en bases separadas
- [ ] Rate limiting responde `429` al superar limite

## Troubleshooting

Si el gateway responde `500` luego de recrear servicios:

```powershell
docker compose up -d --force-recreate api-gateway
```

Si un servicio queda en `starting`:

```powershell
docker compose logs --tail 100 <servicio>
docker inspect <contenedor> --format "{{json .State.Health}}"
```

Si PostgreSQL no crea las bases:

- Verifica si el volumen `platform-postgres-data` ya existia.
- El script de inicializacion solo corre cuando el volumen es nuevo.
- Para reinicializar en desarrollo:

```powershell
docker compose down -v
docker compose up --build -d
```

Si Kafka no responde:

```powershell
docker compose logs --tail 100 kafka
docker exec platform-kafka kafka-broker-api-versions --bootstrap-server kafka:29092
```

Si hay errores de JWT:

- Verifica que todos los servicios usen el mismo `JWT_SECRET`.
- Verifica que el header sea `Authorization: Bearer <token>`.

Si el rate limiting bloquea pruebas repetidas:

- Espera la ventana configurada.
- Usa otro `X-Forwarded-For` para pruebas controladas.

## Pendientes Para Produccion

- Reemplazar `spring.jpa.hibernate.ddl-auto=update` por migraciones con Flyway o Liquibase.
- Usar secrets manager o Docker/Kubernetes secrets para `JWT_SECRET` y `POSTGRES_PASSWORD`.
- Agregar observabilidad centralizada.
- Agregar metricas de aplicacion e infraestructura.
- Agregar tracing distribuido.
- Agregar pipeline CI/CD.
- Preparar despliegue Kubernetes.
- Reemplazar rate limiting en memoria por Redis u otro backend compartido.
- Versionar contratos Kafka.
- Agregar pruebas de contrato REST y Kafka.
- Agregar healthchecks profundos con estado de DB/Kafka.
- Revisar politicas de CORS para entorno real.

## Estado FASE 7

FASE 7 completada: la plataforma queda dockerizada, con configuracion externalizada, infraestructura persistente, red interna, healthchecks, gateway como entrada unica, PostgreSQL separado por servicio y Kafka operativo para comunicacion asincrona.
