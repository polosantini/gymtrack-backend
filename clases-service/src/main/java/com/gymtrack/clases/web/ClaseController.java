package com.gymtrack.clases.web;

import com.gymtrack.clases.application.ClaseService;
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

    public ClaseController(ClaseService claseService) {
        this.claseService = claseService;
    }

    @PostMapping("/clases")
    public ResponseEntity<ClaseResponse> crear(@Valid @RequestBody ClaseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(claseService.crear(request));
    }

    @GetMapping("/clases")
    public ResponseEntity<List<ClaseResponse>> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(claseService.listar(fecha));
    }

    @GetMapping("/clases/{id}")
    public ResponseEntity<ClaseResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(claseService.obtenerPorId(id));
    }
}
