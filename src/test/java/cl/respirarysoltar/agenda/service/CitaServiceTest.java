package cl.respirarysoltar.agenda.service;

import cl.respirarysoltar.agenda.api.dto.CitaRequest;
import cl.respirarysoltar.agenda.domain.Cita;
import cl.respirarysoltar.agenda.domain.EstadoCita;
import cl.respirarysoltar.agenda.exception.ReglaNegocioException;
import cl.respirarysoltar.agenda.repository.CitaRepository;
import cl.respirarysoltar.agenda.repository.HistorialEstadoCitaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CitaServiceTest {

    @Mock
    private CitaRepository citaRepository;

    @Mock
    private HistorialEstadoCitaRepository historialRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private CitaService service;

    private final UUID pacienteId = UUID.randomUUID();
    private final UUID psicologoId = UUID.randomUUID();
    private final LocalDateTime horario = LocalDateTime.now().plusDays(2).withNano(0);

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void reservarHorarioLibreConfirmaLaCita() {
        when(citaRepository.existsByPsicologoIdAndHorarioAndEstadoIn(eq(psicologoId), eq(horario), anyList()))
                .thenReturn(false);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(citaRepository.save(any(Cita.class))).thenAnswer(inv -> inv.getArgument(0));

        Cita creada = service.reservar(new CitaRequest(pacienteId, psicologoId, horario));

        assertNotNull(creada);
        assertEquals(EstadoCita.CONFIRMADA, creada.getEstado());
        assertEquals(1, creada.getHistorial().size());
    }

    @Test
    void reservarSlotOcupadoEnRedisLanzaConflicto() {
        when(citaRepository.existsByPsicologoIdAndHorarioAndEstadoIn(eq(psicologoId), eq(horario), anyList()))
                .thenReturn(false);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

        CitaRequest request = new CitaRequest(pacienteId, psicologoId, horario);
        assertThrows(ReglaNegocioException.class, () -> service.reservar(request));
    }

    @Test
    void reservarSlotOcupadoEnBaseDeDatosLanzaConflicto() {
        when(citaRepository.existsByPsicologoIdAndHorarioAndEstadoIn(eq(psicologoId), eq(horario), anyList()))
                .thenReturn(true);

        CitaRequest request = new CitaRequest(pacienteId, psicologoId, horario);
        assertThrows(ReglaNegocioException.class, () -> service.reservar(request));
    }

    @Test
    void cancelarCitaConfirmadaLaMarcaComoCancelada() {
        Cita cita = new Cita(pacienteId, psicologoId, horario);
        when(citaRepository.findById(any())).thenReturn(Optional.of(cita));
        when(citaRepository.save(any(Cita.class))).thenAnswer(inv -> inv.getArgument(0));

        Cita cancelada = service.cancelar(UUID.randomUUID(), "El paciente no puede asistir");

        assertEquals(EstadoCita.CANCELADA, cancelada.getEstado());
        assertEquals(1, cancelada.getHistorial().size());
    }

    @Test
    void cancelarCitaYaCanceladaLanzaConflicto() {
        Cita cita = new Cita(pacienteId, psicologoId, horario);
        cita.setEstado(EstadoCita.CANCELADA);
        when(citaRepository.findById(any())).thenReturn(Optional.of(cita));

        assertThrows(ReglaNegocioException.class, () -> service.cancelar(UUID.randomUUID(), "motivo"));
    }
}
