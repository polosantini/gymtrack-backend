package com.gymtrack.pagos.application;

import com.gymtrack.common.events.PagoRegistrado;
import com.gymtrack.pagos.domain.exception.PeriodoInvalidoException;
import com.gymtrack.pagos.domain.exception.PlanInvalidoException;
import com.gymtrack.pagos.domain.model.Pago;
import com.gymtrack.pagos.domain.model.PeriodoPago;
import com.gymtrack.pagos.domain.model.PlanPago;
import com.gymtrack.pagos.domain.repository.PagoRepository;
import com.gymtrack.pagos.web.dto.PagoRequest;
import com.gymtrack.pagos.web.dto.PagoResponse;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PagoServiceImpl implements PagoService {

    private final PagoRepository pagoRepository;
    private final ApplicationEventPublisher eventPublisher;

    public PagoServiceImpl(PagoRepository pagoRepository, ApplicationEventPublisher eventPublisher) {
        this.pagoRepository = pagoRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public PagoResponse crearPago(PagoRequest request) {
        PlanPago plan = parsePlan(request.plan());
        PeriodoPago periodo = parsePeriodo(request.periodo());

        Pago pago = new Pago();
        pago.setMiembroId(request.miembroId());
        pago.setPlan(plan);
        pago.setPeriodo(periodo);
        pago.setMonto(request.monto());
        pago.setMetodo(request.metodo());

        Pago guardado = pagoRepository.save(pago);

        eventPublisher.publishEvent(new PagoRegistrado(
                guardado.getId(),
                guardado.getMiembroId(),
                guardado.getPlan().name(),
                guardado.getPeriodo().name(),
                guardado.getMonto(),
                guardado.getEstado().name()
        ));

        return toResponse(guardado);
    }

    @Override
    public List<PagoResponse> listarPorMiembro(Long miembroId) {
        return pagoRepository.findByMiembroId(miembroId).stream().map(this::toResponse).toList();
    }

    private PlanPago parsePlan(String plan) {
        try {
            return PlanPago.valueOf(plan.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new PlanInvalidoException(plan);
        }
    }

    private PeriodoPago parsePeriodo(String periodo) {
        try {
            return PeriodoPago.valueOf(periodo.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new PeriodoInvalidoException(periodo);
        }
    }

    private PagoResponse toResponse(Pago pago) {
        return new PagoResponse(
                pago.getId(),
                pago.getMiembroId(),
                pago.getPlan().name(),
                pago.getPeriodo().name(),
                pago.getMonto(),
                pago.getMetodo(),
                pago.getEstado().name(),
                pago.getFecha()
        );
    }
}