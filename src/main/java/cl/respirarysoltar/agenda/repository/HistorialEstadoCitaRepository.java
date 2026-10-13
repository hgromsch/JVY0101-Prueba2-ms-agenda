package cl.respirarysoltar.agenda.repository;

import cl.respirarysoltar.agenda.domain.HistorialEstadoCita;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface HistorialEstadoCitaRepository extends JpaRepository<HistorialEstadoCita, UUID> {

    List<HistorialEstadoCita> findByCitaIdOrderByFechaAsc(UUID citaId);
}
