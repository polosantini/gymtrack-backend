package com.gymtrack.clases.application;

import com.gymtrack.clases.domain.model.Clase;
import com.gymtrack.clases.domain.model.DiaSemana;
import com.gymtrack.clases.domain.repository.ClaseRepository;
import com.gymtrack.clases.web.dto.ClaseRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalTime;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaseServiceImplTest {
    @Mock private ClaseRepository clases;
    @InjectMocks private ClaseServiceImpl servicio;

    @Test
    void creaClaseSinDependerDelServicioDeReservas() {
        when(clases.save(any(Clase.class))).thenAnswer(inv -> inv.getArgument(0));
        var resultado = servicio.crear(new ClaseRequest("Yoga", "Instructor", Set.of(DiaSemana.MARTES),
                LocalTime.of(18, 0), 45, 5, "Sala"));
        assertEquals("Yoga", resultado.nombre());
        assertEquals(5, resultado.cuposLibres());
        assertEquals(45, resultado.duracionMinutos());
        assertEquals(DiaSemana.MARTES, DiaSemana.de(resultado.fecha()));
    }
}
