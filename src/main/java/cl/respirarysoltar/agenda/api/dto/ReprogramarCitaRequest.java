package cl.respirarysoltar.agenda.api.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/** Datos de entrada para reprogramar una cita existente (RF-05). */
public record ReprogramarCitaRequest(
        @NotNull(message = "El nuevo horario es obligatorio")
        @Future(message = "El nuevo horario debe ser futuro")
        LocalDateTime nuevoHorario
) {
}
