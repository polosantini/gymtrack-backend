package com.gymtrack.pagos.domain.repository;

import com.gymtrack.pagos.domain.model.Notificacion;
import com.gymtrack.pagos.domain.model.TipoNotificacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@DisplayName("NotificacionRepository")
class NotificacionRepositoryTest {

    @Autowired
    private NotificacionRepository notificacionRepository;

    private Notificacion crearNotificacion(Long miembroId, String mensaje, LocalDateTime fecha) {
        Notificacion n = new Notificacion();
        n.setMiembroId(miembroId);
        n.setTipo(TipoNotificacion.CONFIRMACION_PAGO);
        n.setMensaje(mensaje);
        n.setFecha(fecha);
        return n;
    }

    @Test
    @DisplayName("save asigna id automáticamente y leida por defecto es false")
    void saveAsignaIdYLeidaPorDefectoEsFalse() {
        Notificacion guardada = notificacionRepository.save(
                crearNotificacion(1L, "Pago confirmado", LocalDateTime.now())
        );

        assertNotNull(guardada.getId());
        assertFalse(guardada.isLeida());
    }

    @Test
    @DisplayName("findByMiembroIdOrderByFechaDesc devuelve en orden descendente por fecha")
    void findByMiembroIdOrderByFechaDescOrdenaCorrectamente() {
        LocalDateTime base = LocalDateTime.now();
        notificacionRepository.save(
                crearNotificacion(1L, "Antigua", base.minusDays(2))
        );
        notificacionRepository.save(
                crearNotificacion(1L, "Reciente", base)
        );
        notificacionRepository.save(
                crearNotificacion(1L, "Intermedia", base.minusDays(1))
        );
        notificacionRepository.save(
                crearNotificacion(2L, "Otro miembro", base)
        );

        List<Notificacion> notificaciones = notificacionRepository.findByMiembroIdOrderByFechaDesc(1L);

        assertEquals(3, notificaciones.size());
        assertEquals("Reciente", notificaciones.get(0).getMensaje());
        assertEquals("Intermedia", notificaciones.get(1).getMensaje());
        assertEquals("Antigua", notificaciones.get(2).getMensaje());
    }

    @Test
    @DisplayName("findByMiembroIdOrderByFechaDesc devuelve lista vacía si el miembro no tiene notificaciones")
    void sinNotificacionesDevuelveListaVacia() {
        notificacionRepository.save(
                crearNotificacion(1L, "Mensaje", LocalDateTime.now())
        );

        List<Notificacion> notificaciones = notificacionRepository.findByMiembroIdOrderByFechaDesc(999L);

        assertTrue(notificaciones.isEmpty());
    }
}