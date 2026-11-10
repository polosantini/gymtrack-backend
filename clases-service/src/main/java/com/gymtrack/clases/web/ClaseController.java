package com.gymtrack.clases.web;

import com.gymtrack.clases.application.ClaseService;
import com.gymtrack.clases.application.ClaseQueryService;
import com.gymtrack.clases.web.dto.ReservasClaseResponse;
import com.gymtrack.clases.web.dto.ClaseRequest;
import com.gymtrack.clases.web.dto.ClaseResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ClaseController {

    private final ClaseService claseService;

    private final ClaseQueryService claseQueryService;

    public ClaseController(ClaseService claseService, ClaseQueryService claseQueryService) {
        this.claseService = claseService;
        this.claseQueryService = claseQueryService;
    }

    @PostMapping("/clases")
    public ResponseEntity<ClaseResponse> crear(@Valid @RequestBody ClaseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(claseService.crear(request));
    }

    @GetMapping("/clases")
    public ResponseEntity<List<ClaseResponse>> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(claseQueryService.listar(fecha));
    }

    @GetMapping("/clases/{id}")
    public ResponseEntity<ClaseResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(claseQueryService.obtenerPorId(id));
    }
    @GetMapping("/clases/{claseId}/reservas")
    public ResponseEntity<ReservasClaseResponse> listarPorClaseYFecha(
            @PathVariable Long claseId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(claseQueryService.listarPorClaseYFecha(claseId, fecha));
    }

}
