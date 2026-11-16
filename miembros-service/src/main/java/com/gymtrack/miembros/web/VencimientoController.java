package com.gymtrack.miembros.web;

import com.gymtrack.miembros.application.VencimientoProximoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/membresias")
public class VencimientoController {

    private final VencimientoProximoService service;

    public VencimientoController(VencimientoProximoService service) {
        this.service = service;
    }

    @PostMapping("/procesar-vencimientos")
    public ResponseEntity<Map<String, Object>> procesar(
            @RequestParam(required = false) Integer ventanaDias) {

        int publicados = service.procesar(ventanaDias);

        return ResponseEntity.ok(Map.of(
                "eventosPublicados", publicados,
                "ventanaDias", ventanaDias != null ? ventanaDias : "default"
        ));
    }
}