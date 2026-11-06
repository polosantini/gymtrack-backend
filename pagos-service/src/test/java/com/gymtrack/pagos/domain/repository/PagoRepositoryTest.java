package com.gymtrack.pagos.domain.repository;

import com.gymtrack.pagos.domain.model.EstadoPago;
import com.gymtrack.pagos.domain.model.Pago;
import com.gymtrack.pagos.domain.model.PeriodoPago;
import com.gymtrack.pagos.domain.model.PlanPago;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@DisplayName("PagoRepository")
class PagoRepositoryTest {

    @Autowired
    private PagoRepository pagoRepository;

    private Pago crearPago(Long miembroId) {
        Pago p = new Pago();
        p.setMiembroId(miembroId);
        p.setPlan(PlanPago.BASICO);
        p.setPeriodo(PeriodoPago.MENSUAL);
        p.setMonto(new BigDecimal("50000.00"));
        p.setMetodo("EFECTIVO");
        return p;
    }

    @Test
    @DisplayName("save asigna id automáticamente")
    void saveAsignaId() {
        Pago guardado = pagoRepository.save(crearPago(1L));

        assertNotNull(guardado.getId());
        assertEquals(EstadoPago.REGISTRADO, guardado.getEstado());
        assertNotNull(guardado.getFecha());
    }

    @Test
    @DisplayName("findByMiembroId filtra solo los pagos del miembro correcto")
    void findByMiembroIdFiltraCorrectamente() {
        pagoRepository.save(crearPago(1L));
        pagoRepository.save(crearPago(1L));
        pagoRepository.save(crearPago(2L));

        List<Pago> pagosMiembro1 = pagoRepository.findByMiembroId(1L);

        assertEquals(2, pagosMiembro1.size());
        assertTrue(pagosMiembro1.stream().allMatch(p -> p.getMiembroId().equals(1L)));
    }

    @Test
    @DisplayName("findByMiembroId devuelve lista vacía si el miembro no tiene pagos")
    void findByMiembroIdSinPagosDevuelveListaVacia() {
        pagoRepository.save(crearPago(1L));

        List<Pago> pagos = pagoRepository.findByMiembroId(999L);

        assertTrue(pagos.isEmpty());
    }
}