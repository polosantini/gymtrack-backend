package com.gymtrack.clases.application;

import com.gymtrack.clases.domain.exception.ClaseNotFoundException;
import com.gymtrack.clases.domain.model.Clase;
import com.gymtrack.clases.domain.model.DiaSemana;
import com.gymtrack.clases.domain.model.EstadoReserva;
import com.gymtrack.clases.domain.repository.ClaseRepository;
import com.gymtrack.clases.domain.repository.ReservaRepository;
import com.gymtrack.clases.web.dto.ClaseResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaseServiceImplTest {

    private static final LocalDate MARTES_17 = LocalDate.of(2026, 11, 17);

    @Mock
    private ClaseRepository claseRepository;

    @Mock
    private ReservaRepository reservaRepository;

    @InjectMocks
    private ClaseServiceImpl claseService;

    private Clase clase(Long id, String nombre, int aforo, DiaSemana... dias) {
        Clase clase = new Clase();
        ReflectionTestUtils.setField(clase, "id", id);
        clase.setNombre(nombre);
        clase.setInstructor("Instructor");
        clase.setDias(Set.of(dias));
        clase.setHora(LocalTime.of(18, 0));
        clase.setDuracionMinutos(45);
        clase.setAforo(aforo);
        clase.setSala("Sala 1");
        return clase;
    }

    @Test
    @DisplayName("Con fecha: solo clases de ese día y cupos = aforo - confirmadas")
    void listarConFechaCalculaCupos() {
        Clase spinning = clase(1L, "Spinning", 3, DiaSemana.MARTES, DiaSemana.JUEVES);
        Clase funcional = clase(2L, "Funcional", 15, DiaSemana.LUNES, DiaSemana.MIERCOLES);
        when(claseRepository.findAll()).thenReturn(List.of(spinning, funcional));
        when(reservaRepository.countByClaseIdAndFechaAndEstado(1L, MARTES_17, EstadoReserva.CONFIRMADA)).thenReturn(2L);

        List<ClaseResponse> catalogo = claseService.listar(MARTES_17);

        assertEquals(1, catalogo.size());
        ClaseResponse respuesta = catalogo.get(0);
        assertEquals("Spinning", respuesta.nombre());
        assertEquals(MARTES_17, respuesta.fecha());
        assertEquals(1, respuesta.cuposLibres());
    }

    @Test
    @DisplayName("Los cupos libres nunca son negativos")
    void cuposNoNegativos() {
        Clase spinning = clase(1L, "Spinning", 3, DiaSemana.MARTES);
        when(claseRepository.findAll()).thenReturn(List.of(spinning));
        when(reservaRepository.countByClaseIdAndFechaAndEstado(1L, MARTES_17, EstadoReserva.CONFIRMADA)).thenReturn(5L);

        assertEquals(0, claseService.listar(MARTES_17).get(0).cuposLibres());
    }

    @Test
    @DisplayName("obtenerPorId lanza ClaseNotFoundException si la clase no existe")
    void obtenerPorIdInexistente() {
        when(claseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ClaseNotFoundException.class, () -> claseService.obtenerPorId(99L));
    }
}
