package cl.respirarysoltar.agenda.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Cita agendada entre un paciente y un psicólogo (RF-04, RF-05).
 * Invariante crítico: nunca más de una cita activa en el mismo horario
 * con el mismo psicólogo; se protege con reserva atómica en Redis (CitaService).
 */
@Entity
@Table(name = "citas")
public class Cita {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID pacienteId;

    @Column(nullable = false)
    private UUID psicologoId;

    @Column(nullable = false)
    private LocalDateTime horario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoCita estado;

    @Column(nullable = false)
    private LocalDateTime creadoEn;

    /** Relación OneToMany: auditoría de todos los cambios de estado de la cita. */
    @OneToMany(mappedBy = "cita", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HistorialEstadoCita> historial = new ArrayList<>();

    protected Cita() {
        // Constructor requerido por JPA
    }

    public Cita(UUID pacienteId, UUID psicologoId, LocalDateTime horario) {
        this.pacienteId = pacienteId;
        this.psicologoId = psicologoId;
        this.horario = horario;
        this.estado = EstadoCita.CONFIRMADA;
    }

    @PrePersist
    void alCrear() {
        if (creadoEn == null) {
            creadoEn = LocalDateTime.now();
        }
    }

    public void registrarCambioEstado(String estadoAnterior, String motivo) {
        HistorialEstadoCita registro = new HistorialEstadoCita(estadoAnterior, estado.name(), motivo);
        registro.setCita(this);
        historial.add(registro);
    }

    public UUID getId() {
        return id;
    }

    public UUID getPacienteId() {
        return pacienteId;
    }

    public UUID getPsicologoId() {
        return psicologoId;
    }

    public LocalDateTime getHorario() {
        return horario;
    }

    public void setHorario(LocalDateTime horario) {
        this.horario = horario;
    }

    public EstadoCita getEstado() {
        return estado;
    }

    public void setEstado(EstadoCita estado) {
        this.estado = estado;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public List<HistorialEstadoCita> getHistorial() {
        return historial;
    }
}
