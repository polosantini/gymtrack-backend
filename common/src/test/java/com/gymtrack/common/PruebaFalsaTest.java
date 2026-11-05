package com.gymtrack.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

class PruebaFalsaTest {
    @Test
    void debeFallar() {
        fail("Prueba intencional para verificar que la CI falla");
    }
}