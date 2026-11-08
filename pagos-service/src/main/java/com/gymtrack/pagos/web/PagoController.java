package com.gymtrack.pagos.web;

import com.gymtrack.pagos.application.NotificacionService;
import com.gymtrack.pagos.application.PagoService;
import com.gymtrack.pagos.web.dto.NotificacionResponse;
import com.gymtrack.pagos.web.dto.PagoRequest;
import com.gymtrack.pagos.web.dto.PagoResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class PagoController {

    private final PagoService pagoService;
    private final NotificacionService notificacionService;

    public PagoController(PagoService pagoService, NotificacionService notificacionService) {
        this.pagoService = pagoService;
        this.notificacionService = notificacionService;
    }

    @PostMapping("/pagos")
    public ResponseEntity<PagoResponse> crearPago(@Valid @RequestBody PagoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pagoService.crearPago(request));
    }

    @GetMapping("/pagos")
    public ResponseEntity<List<PagoResponse>> listarPagos(@RequestParam Long miembroId) {
        return ResponseEntity.ok(pagoService.listarPorMiembro(miembroId));
    }


    @GetMapping("/notificaciones")
    public ResponseEntity<List<NotificacionResponse>> listarNotificaciones(@RequestParam Long miembroId) {
        return ResponseEntity.ok(notificacionService.listarPorMiembro(miembroId));
    }
}
