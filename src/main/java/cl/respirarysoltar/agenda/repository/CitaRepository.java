package cl.respirarysoltar.agenda.repository;

import cl.respirarysoltar.agenda.domain.Cita;
import cl.respirarysoltar.agenda.domain.EstadoCita;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface CitaRepository extends JpaRepository<Cita, UUID> {

    List<Cita> findByPacienteId(UUID pacienteId);

    List<Cita> findByPsicologoId(UUID psicologoId);

    List<Cita> findByEstado(EstadoCita estado);

    /** Respaldo del invariante anti doble-booking a nivel base de datos. */
    boolean existsByPsicologoIdAndHorarioAndEstadoIn(UUID psicologoId, LocalDateTime horario, List<EstadoCita> estados);
}
