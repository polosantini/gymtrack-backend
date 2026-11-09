package com.gymtrack.miembros.web;

import com.gymtrack.miembros.application.MiembroService;
import com.gymtrack.miembros.web.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api")
public class MiembroController {

    private final MiembroService miembroService;

    public MiembroController(MiembroService miembroService) {
        this.miembroService = miembroService;
    }

    @PostMapping("/miembros")
    public ResponseEntity<MiembroResponse> registrar(@Valid @RequestBody MiembroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(miembroService.registrar(request));
    }

    @GetMapping("/miembros")
    public ResponseEntity<List<MiembroResponse>> listar() {
        return ResponseEntity.ok(miembroService.listar());
    }

    @GetMapping("/miembros/{id}")
    public ResponseEntity<MiembroResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(miembroService.obtenerPorId(id));
    }

    @PostMapping("/miembros/{id}/membresias")
    public ResponseEntity<MembresiaResponse> crearMembresia(@PathVariable Long id, @Valid @RequestBody MembresiaRequest request){
        return ResponseEntity.status(HttpStatus.CREATED).body(miembroService.crearMembresia(id, request));
    }

    @GetMapping("/planes")
    public ResponseEntity<List<PlanResponse>> listarPlanes() {
        return ResponseEntity.ok(miembroService.listarPlanes());
    }

    @GetMapping("/miembros/{id}/acceso")
    public ResponseEntity<AccesoResponse> verificarAcceso(@PathVariable Long id) {
        return ResponseEntity.ok(miembroService.verificarAcceso(id));
    }
}
