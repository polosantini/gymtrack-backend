package com.gymtrack.clases.application;
import com.gymtrack.clases.application.port.BloqueoReservasPorMiembro;
import com.gymtrack.clases.domain.strategy.ReglasReservaStrategy;
import com.gymtrack.clases.domain.model.Reserva;
import com.gymtrack.clases.web.dto.ReservaRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class ReservaConTransaccion {
 private final ReservarClaseUseCaseFactory fabrica;
 private final BloqueoReservasPorMiembro bloqueo;
 public ReservaConTransaccion(ReservarClaseUseCaseFactory f,BloqueoReservasPorMiembro b) {fabrica=f;bloqueo=b;}
 @Transactional
 public Reserva ejecutar(ReglasReservaStrategy regla,ReservaRequest request) {
  bloqueo.adquirir(request.miembroId());
  return fabrica.crear(regla).ejecutar(request);
 }
}
