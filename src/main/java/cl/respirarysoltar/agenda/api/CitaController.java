package cl.respirarysoltar.agenda.api;

import cl.respirarysoltar.agenda.api.dto.CancelarCitaRequest;
import cl.respirarysoltar.agenda.api.dto.CitaRequest;
import cl.respirarysoltar.agenda.api.dto.ReprogramarCitaRequest;
import cl.respirarysoltar.agenda.domain.Cita;
import cl.respirarysoltar.agenda.domain.EstadoCita;
import cl.respirarysoltar.agenda.domain.HistorialEstadoCita;
import cl.respirarysoltar.agenda.service.CitaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * API REST de agendamiento de citas (RF-04) y de reprogramación/cancelación (RF-05).
 * El invariante anti doble-booking se protege en CitaService con reserva atómica en Redis.
 */
@RestController
@RequestMapping("/api/citas")
public class CitaController {

    private final CitaService service;

    public CitaController(CitaService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Cita> reservar(@Valid @RequestBody CitaRequest request) {
        Cita creada = service.reservar(request);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creada.getId()).toUri();
        return ResponseEntity.created(ubicacion).body(creada);
    }

    @GetMapping
    public List<Cita> listar(@RequestParam(required = false) UUID pacienteId,
                             @RequestParam(required = false) UUID psicologoId,
                             @RequestParam(required = false) EstadoCita estado) {
        return service.listar(pacienteId, psicologoId, estado);
    }

    @GetMapping("/{id}")
    public Cita obtener(@PathVariable UUID id) {
        return service.obtener(id);
    }

    @PostMapping("/{id}/cancelar")
    public Cita cancelar(@PathVariable UUID id,
                         @RequestBody(required = false) CancelarCitaRequest request) {
        String motivo = request == null ? null : request.motivo();
        return service.cancelar(id, motivo);
    }

    @PostMapping("/{id}/reprogramar")
    public Cita reprogramar(@PathVariable UUID id, @Valid @RequestBody ReprogramarCitaRequest request) {
        return service.reprogramar(id, request.nuevoHorario());
    }

    @GetMapping("/{id}/historial")
    public List<HistorialEstadoCita> historial(@PathVariable UUID id) {
        return service.historial(id);
    }
}
