package com.gymtrack.clases.web;

import com.gymtrack.clases.application.ReservaService;
import com.gymtrack.clases.web.dto.AsistenciaRequest;
import com.gymtrack.clases.web.dto.ReservaRequest;
import com.gymtrack.clases.web.dto.ReservaResponse;
import com.gymtrack.clases.web.dto.ReservasClaseResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ReservaController {

    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @PostMapping("/reservas")
    public ResponseEntity<ReservaResponse> reservar(@Valid @RequestBody ReservaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservaService.reservar(request));
    }

    @GetMapping("/reservas")
    public ResponseEntity<List<ReservaResponse>> listarPorMiembro(@RequestParam Long miembroId) {
        return ResponseEntity.ok(reservaService.listarPorMiembro(miembroId));
    }

    @GetMapping("/clases/{claseId}/reservas")
    public ResponseEntity<ReservasClaseResponse> listarPorClaseYFecha(
            @PathVariable Long claseId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(reservaService.listarPorClaseYFecha(claseId, fecha));
    }

    @PatchMapping("/reservas/{id}/cancelar")
    public ResponseEntity<ReservaResponse> cancelar(@PathVariable Long id) {
        return ResponseEntity.ok(reservaService.cancelar(id));
    }

    @PatchMapping("/reservas/{id}/asistencia")
    public ResponseEntity<ReservaResponse> marcarAsistencia(@PathVariable Long id,
                                                            @Valid @RequestBody AsistenciaRequest request) {
        return ResponseEntity.ok(reservaService.marcarAsistencia(id, request.asistio()));
    }
}
