# ms-agenda

Agendamiento, reprogramación y cancelación de citas. Cubre **RF-04 y RF-05**. Protege el
invariante de negocio más crítico del sistema: **nunca más de un paciente por horario con
el mismo psicólogo**. La reserva se hace de forma atómica en Redis (`SETNX` + TTL) con doble
verificación en la base de datos; ver `CitaService.reservar`.

**Puerto:** 8083 · **Base de datos:** `agenda_db` · **Swagger:** http://localhost:8083/swagger-ui.html

## Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/citas` | Agenda una cita (201; 409 si el horario está tomado) |
| GET | `/api/citas?pacienteId=&psicologoId=&estado=` | Lista citas con filtros opcionales |
| GET | `/api/citas/{id}` | Obtiene una cita (404 si no existe) |
| POST | `/api/citas/{id}/cancelar` | Cancela la cita y libera el slot |
| POST | `/api/citas/{id}/reprogramar` | Reprograma a un nuevo horario (re-reserva atómica) |
| GET | `/api/citas/{id}/historial` | Auditoría de cambios de estado |

Estados: `CONFIRMADA`, `REPROGRAMADA`, `CANCELADA`, `REALIZADA`.

## Cómo correr localmente

```bash
docker compose -f ../docker-compose.yml up -d   # Postgres + Redis (Redis es OBLIGATORIO aquí)
mvn spring-boot:run
```

## Arquitectura

Capas `api` → `service` → `repository` → `domain`. En AWS llamará de forma síncrona con
Circuit Breaker a `ms-psicologos` (valida disponibilidad) y a `ms-pagos` (cobra la sesión),
y publicará eventos a SQS (`ms-notificaciones`) y EventBridge.
Relación JPA destacada: `Cita` **OneToMany** `HistorialEstadoCita` (auditoría, **ManyToOne** inversa).

## Despliegue

Pensado para **Amazon ECS Fargate** con Aurora Serverless y ElastiCache (Redis).
Imagen: `docker build -t ms-agenda .`
