package cl.respirarysoltar.agenda.domain;

/** Ciclo de vida de una cita agendada (RF-04, RF-05). */
public enum EstadoCita {
    CONFIRMADA,
    REPROGRAMADA,
    CANCELADA,
    REALIZADA
}
