package com.gymtrack.miembros.domain.repository;

import com.gymtrack.miembros.domain.model.Miembro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MiembroRepository extends JpaRepository<Miembro, Long> {
    Optional<Miembro> findByEmail(String email);
    boolean existsByEmail(String email);
}