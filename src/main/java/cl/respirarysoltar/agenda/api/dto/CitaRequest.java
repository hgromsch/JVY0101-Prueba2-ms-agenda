package cl.respirarysoltar.agenda.api.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

/** Datos de entrada para agendar una nueva cita (RF-04). */
public record CitaRequest(
        @NotNull(message = "El paciente es obligatorio")
        UUID pacienteId,

        @NotNull(message = "El psicólogo es obligatorio")
        UUID psicologoId,

        @NotNull(message = "El horario es obligatorio")
        @Future(message = "El horario de la cita debe ser futuro")
        LocalDateTime horario
) {
}
