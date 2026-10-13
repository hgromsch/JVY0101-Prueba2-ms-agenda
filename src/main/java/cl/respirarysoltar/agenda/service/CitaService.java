package cl.respirarysoltar.agenda.service;

import cl.respirarysoltar.agenda.api.dto.CitaRequest;
import cl.respirarysoltar.agenda.domain.Cita;
import cl.respirarysoltar.agenda.domain.EstadoCita;
import cl.respirarysoltar.agenda.domain.HistorialEstadoCita;
import cl.respirarysoltar.agenda.exception.RecursoNoEncontradoException;
import cl.respirarysoltar.agenda.exception.ReglaNegocioException;
import cl.respirarysoltar.agenda.repository.CitaRepository;
import cl.respirarysoltar.agenda.repository.HistorialEstadoCitaRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Agendamiento, reprogramación y cancelación de citas (RF-04, RF-05).
 *
 * Implementa el invariante de negocio más crítico del sistema:
 * "nunca se agenda más de un paciente en el mismo horario con el mismo psicólogo".
 *
 * La reserva se hace primero de forma atómica en Redis (SETNX + TTL) antes de
 * escribir en la base de datos transaccional. Si el SETNX falla, significa que
 * otro paciente ya está reservando ese mismo slot en este instante: se rechaza
 * de inmediato en vez de arriesgar una condición de carrera contra la base de datos.
 * Como respaldo se verifica también en la base de datos (doble verificación).
 */
@Service
@Transactional
public class CitaService {

    private static final Duration TTL_RESERVA = Duration.ofMinutes(10);
    private static final List<EstadoCita> ESTADOS_ACTIVOS = List.of(EstadoCita.CONFIRMADA, EstadoCita.REPROGRAMADA);

    private final CitaRepository citaRepository;
    private final HistorialEstadoCitaRepository historialRepository;
    private final StringRedisTemplate redisTemplate;

    public CitaService(CitaRepository citaRepository,
                       HistorialEstadoCitaRepository historialRepository,
                       StringRedisTemplate redisTemplate) {
        this.citaRepository = citaRepository;
        this.historialRepository = historialRepository;
        this.redisTemplate = redisTemplate;
    }

    /** RF-04: agenda una cita garantizando que el horario no esté tomado. */
    public Cita reservar(CitaRequest request) {
        validarSlotDisponible(request.psicologoId(), request.horario());

        Cita cita = new Cita(request.pacienteId(), request.psicologoId(), request.horario());
        cita.registrarCambioEstado(null, "Reserva inicial de la cita");
        Cita guardada = citaRepository.save(cita);

        // TODO (AWS): publicar evento "cita_confirmada" a la cola SQS de ms-notificaciones.
        return guardada;
    }

    @Transactional(readOnly = true)
    public List<Cita> listar(UUID pacienteId, UUID psicologoId, EstadoCita estado) {
        if (pacienteId != null) {
            return citaRepository.findByPacienteId(pacienteId);
        }
        if (psicologoId != null) {
            return citaRepository.findByPsicologoId(psicologoId);
        }
        if (estado != null) {
            return citaRepository.findByEstado(estado);
        }
        return citaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Cita obtener(UUID id) {
        return citaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cita no encontrada: " + id));
    }

    /** RF-05: cancela la cita, libera el slot en Redis y deja trazabilidad. */
    public Cita cancelar(UUID id, String motivo) {
        Cita cita = obtener(id);
        if (cita.getEstado() == EstadoCita.CANCELADA) {
            throw new ReglaNegocioException("La cita ya está cancelada");
        }
        if (cita.getEstado() == EstadoCita.REALIZADA) {
            throw new ReglaNegocioException("No se puede cancelar una cita ya realizada");
        }

        String estadoAnterior = cita.getEstado().name();
        cita.setEstado(EstadoCita.CANCELADA);
        cita.registrarCambioEstado(estadoAnterior, motivo == null || motivo.isBlank() ? "Sin motivo informado" : motivo);
        liberarSlot(cita.getPsicologoId(), cita.getHorario());

        // TODO (AWS): publicar evento "cita_cancelada" a EventBridge para gatillar
        // el reembolso en ms-pagos y la notificación en ms-notificaciones.
        return citaRepository.save(cita);
    }

    /** RF-05: mueve la cita a un nuevo horario, re-reservando el slot atómicamente. */
    public Cita reprogramar(UUID id, LocalDateTime nuevoHorario) {
        Cita cita = obtener(id);
        if (cita.getEstado() != EstadoCita.CONFIRMADA && cita.getEstado() != EstadoCita.REPROGRAMADA) {
            throw new ReglaNegocioException("Solo se pueden reprogramar citas confirmadas");
        }
        validarSlotDisponible(cita.getPsicologoId(), nuevoHorario);

        LocalDateTime horarioAnterior = cita.getHorario();
        liberarSlot(cita.getPsicologoId(), horarioAnterior);

        String estadoAnterior = cita.getEstado().name();
        cita.setHorario(nuevoHorario);
        cita.setEstado(EstadoCita.REPROGRAMADA);
        cita.registrarCambioEstado(estadoAnterior, "Reprogramada desde " + horarioAnterior);

        // TODO (AWS): publicar evento "cita_reprogramada" a SQS para ms-notificaciones.
        return citaRepository.save(cita);
    }

    @Transactional(readOnly = true)
    public List<HistorialEstadoCita> historial(UUID citaId) {
        obtener(citaId);
        return historialRepository.findByCitaIdOrderByFechaAsc(citaId);
    }

    /**
     * Doble verificación del invariante anti doble-booking:
     * 1) la base de datos no tiene una cita activa en ese slot, y
     * 2) la reserva atómica en Redis (SETNX con TTL) tiene éxito.
     */
    private void validarSlotDisponible(UUID psicologoId, LocalDateTime horario) {
        if (citaRepository.existsByPsicologoIdAndHorarioAndEstadoIn(psicologoId, horario, ESTADOS_ACTIVOS)) {
            throw new ReglaNegocioException("El horario " + horario + " ya está tomado para este psicólogo");
        }
        String key = claveSlot(psicologoId, horario);
        Boolean reservado = redisTemplate.opsForValue().setIfAbsent(key, "reservado", TTL_RESERVA);
        if (Boolean.FALSE.equals(reservado)) {
            throw new ReglaNegocioException("El horario " + horario + " está siendo reservado por otro paciente en este momento");
        }
    }

    private void liberarSlot(UUID psicologoId, LocalDateTime horario) {
        redisTemplate.delete(claveSlot(psicologoId, horario));
    }

    private String claveSlot(UUID psicologoId, LocalDateTime horario) {
        return "slot:%s:%s".formatted(psicologoId, horario);
    }
}
