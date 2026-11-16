package com.gymtrack.miembros.domain.repository;

import com.gymtrack.miembros.domain.model.Miembro;
import org.springframework.data.jpa.repository.JpaRepository;
import com.gymtrack.miembros.domain.model.EstadoMembresia;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

import java.util.Optional;

public interface MiembroRepository extends JpaRepository<Miembro, Long> {
    Optional<Miembro> findByEmail(String email);
    boolean existsByEmail(String email);
    @Query("""
        SELECT m FROM Miembro m
        JOIN FETCH m.membresia mem
        WHERE mem.estado = :estado
          AND mem.fechaVencimiento BETWEEN :desde AND :hasta
        """)
    List<Miembro> findMiembrosConMembresiaVenciendoEnVentana(
            @Param("estado") EstadoMembresia estado,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);
}