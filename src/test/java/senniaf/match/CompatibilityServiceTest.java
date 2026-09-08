package senniaf.match;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CompatibilityServiceTest {

    @Test
    void debeSerCompatibleCuandoElPuntajeEsMayorOIgualA70() {

        CompatibilityService service = new CompatibilityService();

        assertTrue(service.isCompatible(70));
        assertTrue(service.isCompatible(90));
    }

    @Test
    void noDebeSerCompatibleCuandoElPuntajeEsMenorA70() {

        CompatibilityService service = new CompatibilityService();

        assertFalse(service.isCompatible(69));
    }
}