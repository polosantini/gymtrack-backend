package com.gymtrack.pagos.application;

import com.gymtrack.common.events.PagoRegistrado;
import com.gymtrack.pagos.domain.model.EstadoPago;
import com.gymtrack.pagos.domain.model.Pago;
import com.gymtrack.pagos.domain.model.PeriodoPago;
import com.gymtrack.pagos.domain.model.PlanPago;
import com.gymtrack.pagos.domain.repository.PagoRepository;
import com.gymtrack.pagos.web.dto.PagoRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PagoServiceImpl.crearPago — publica PagoRegistrado")
class PagoServiceImplTest {

    @Mock
    private PagoRepository pagoRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PagoServiceImpl service;

    @Test
    @DisplayName("Al guardar el pago publica un PagoRegistrado con los campos del contrato")
    void crearPago_guardaYPublicaEventoConCamposDelContrato() {
        PagoRequest request = new PagoRequest(
                1L,
                "BASICO",
                "MENSUAL",
                new BigDecimal("50000.00"),
                "EFECTIVO"
        );

        Pago guardado = new Pago();
        ReflectionTestUtils.setField(guardado, "id", 10L);
        guardado.setMiembroId(1L);
        guardado.setPlan(PlanPago.BASICO);
        guardado.setPeriodo(PeriodoPago.MENSUAL);
        guardado.setMonto(new BigDecimal("50000.00"));
        guardado.setMetodo("EFECTIVO");
        guardado.setEstado(EstadoPago.REGISTRADO);

        when(pagoRepository.save(any(Pago.class))).thenReturn(guardado);

        service.crearPago(request);

        ArgumentCaptor<PagoRegistrado> captor = ArgumentCaptor.forClass(PagoRegistrado.class);
        verify(eventPublisher).publishEvent(captor.capture());

        PagoRegistrado evento = captor.getValue();
        assertThat(evento.pagoId()).isEqualTo(10L);
        assertThat(evento.miembroId()).isEqualTo(1L);
        assertThat(evento.plan()).isEqualTo("BASICO");
        assertThat(evento.periodo()).isEqualTo("MENSUAL");
        assertThat(evento.monto()).isEqualByComparingTo("50000.00");
        assertThat(evento.estado()).isEqualTo("REGISTRADO");
    }
}