package com.gymtrack.clases.infrastructure.persistence;
import com.gymtrack.clases.application.port.BloqueoReservasPorMiembro;
import com.gymtrack.clases.domain.repository.CandadoMiembroRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;
@Component
public class BloqueoReservasJpa implements BloqueoReservasPorMiembro {
 private final CrearCandadoMiembro creador;
 private final CandadoMiembroRepository repo;
 private final Object[] creacionLocal=java.util.stream.IntStream.range(0,256).mapToObj(i->new Object()).toArray();
 public BloqueoReservasJpa(CrearCandadoMiembro creador,CandadoMiembroRepository repo) {this.creador=creador;this.repo=repo;}
 @Override public void preparar(Long id) {
  synchronized(creacionLocal[Math.floorMod(id.hashCode(),creacionLocal.length)]) {
   try {creador.preparar(id);}catch(DataIntegrityViolationException ex) {
    if(!repo.existsById(id))throw ex; // Otra instancia creo la fila en su propia transaccion.
   }
  }
 }
 @Override @Transactional(propagation=Propagation.MANDATORY)
 public void adquirir(Long id) {
  repo.bloquear(id).orElseThrow(()->new IllegalStateException("No se pudo bloquear el miembro"));
 }
}
