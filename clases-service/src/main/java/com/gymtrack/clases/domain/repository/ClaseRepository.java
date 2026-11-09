package com.gymtrack.clases.domain.repository;

import com.gymtrack.clases.domain.model.Clase;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClaseRepository extends JpaRepository<Clase, Long> {
}
