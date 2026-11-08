package com.gymtrack.miembros.domain.repository;

import com.gymtrack.miembros.domain.model.Miembro;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = "spring.sql.init.mode=never")
@DisplayName("MiembroRepository")
class MiembroRepositoryTest {

    @Autowired
    private MiembroRepository miembroRepository;

    // Método helper (opcional, pero útil): crea un Miembro con datos válidos
    private Miembro crearMiembro(String nombre, String email) {
        Miembro m = new Miembro();
        m.setNombre(nombre);
        m.setEmail(email);
        // fechaRegistro se llena con @PrePersist al guardar
        return m;
    }

    @Test
    @DisplayName("Guarda un miembro y le asigna id y fechaRegistro automáticamente")
    void guardaMiembroYLeAsignaId() {
        Miembro guardado = miembroRepository.save(crearMiembro("Ana", "ana@test.com"));

        assertNotNull(guardado.getId());
        assertNotNull(guardado.getFechaRegistro());
    }

    @Test
    @DisplayName("findByEmail encuentra al miembro correcto")
    void findByEmailEncuentraMiembro() {
        miembroRepository.save(crearMiembro("Luis", "luis@test.com"));

        Optional<Miembro> resultado = miembroRepository.findByEmail("luis@test.com");

        assertTrue(resultado.isPresent());
        assertEquals("Luis", resultado.get().getNombre());
    }

    @Test
    @DisplayName("existsByEmail devuelve true para email existente y false para inexistente")
    void existsByEmailFunciona() {
        miembroRepository.save(crearMiembro("Sara", "sara@test.com"));

        assertTrue(miembroRepository.existsByEmail("sara@test.com"));
        assertFalse(miembroRepository.existsByEmail("noexiste@test.com"));
    }

    @Test
    @DisplayName("No permite guardar dos miembros con el mismo email")
    void emailUnicoEsRespetado() {
        miembroRepository.saveAndFlush(crearMiembro("Pedro", "pedro@test.com"));

        Miembro duplicado = crearMiembro("Otro Pedro", "pedro@test.com");

        assertThrows(DataIntegrityViolationException.class, () ->
                miembroRepository.saveAndFlush(duplicado)
        );
    }
}
