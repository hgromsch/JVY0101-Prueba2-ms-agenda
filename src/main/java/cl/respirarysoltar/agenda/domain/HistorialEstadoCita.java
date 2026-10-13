package cl.respirarysoltar.agenda.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Registro de auditoría de cada cambio de estado de una cita (lado "muchos" de
 * ManyToOne). Permite trazabilidad completa del ciclo de vida de la reserva.
 */
@Entity
@Table(name = "historial_estados_cita")
public class HistorialEstadoCita {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(length = 15)
    private String estadoAnterior;

    @Column(nullable = false, length = 15)
    private String estadoNuevo;

    @Column(length = 250)
    private String motivo;

    @Column(nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cita_id", nullable = false)
    private Cita cita;

    protected HistorialEstadoCita() {
        // Constructor requerido por JPA
    }

    public HistorialEstadoCita(String estadoAnterior, String estadoNuevo, String motivo) {
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.motivo = motivo;
    }

    public UUID getId() {
        return id;
    }

    public String getEstadoAnterior() {
        return estadoAnterior;
    }

    public String getEstadoNuevo() {
        return estadoNuevo;
    }

    public String getMotivo() {
        return motivo;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public Cita getCita() {
        return cita;
    }

    public void setCita(Cita cita) {
        this.cita = cita;
    }
}
