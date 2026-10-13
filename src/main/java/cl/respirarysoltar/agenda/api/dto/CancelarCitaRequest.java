package cl.respirarysoltar.agenda.api.dto;

import jakarta.validation.constraints.Size;

/** Datos de entrada para cancelar una cita (RF-05). El motivo es opcional. */
public record CancelarCitaRequest(
        @Size(max = 250)
        String motivo
) {
}
