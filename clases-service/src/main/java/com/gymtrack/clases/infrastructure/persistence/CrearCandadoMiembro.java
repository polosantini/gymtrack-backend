package com.gymtrack.clases.infrastructure.persistence;
import com.gymtrack.clases.domain.model.CandadoMiembro;
import com.gymtrack.clases.domain.repository.CandadoMiembroRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;
@Component
public class CrearCandadoMiembro {
 private final CandadoMiembroRepository repo;
 public CrearCandadoMiembro(CandadoMiembroRepository repo) {this.repo=repo;}
 @Transactional(propagation=Propagation.REQUIRES_NEW)
 public void preparar(Long id) {if(!repo.existsById(id))repo.saveAndFlush(new CandadoMiembro(id));}
}
