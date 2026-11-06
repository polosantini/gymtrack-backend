package com.gymtrack.miembros.domain.repository;

import com.gymtrack.miembros.domain.model.Membresia;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembresiaRepository extends JpaRepository<Membresia, Long> {
}