package com.senniaf.match.service;

import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import static org.junit.jupiter.api.Assertions.*;

class HorarioCitasTest {
    final Clock reloj=Clock.fixed(Instant.parse("2026-11-23T15:15:00Z"),ZoneOffset.UTC);
    final HorarioCitas horario=new HorarioCitas("1@08:00-16:00,2@08:00-16:00,3@08:00-16:00,4@08:00-16:00,5@08:00-16:00",30);
    @ParameterizedTest
    @CsvSource(delimiter='|',value={
        "2026-11-22|10:00|120", "2026-02-30|10:00|120", "2026-11-23|09:00|120",
        "2026-11-23|10:00|120", "2026-11-24|25:00|120", "2026-11-24|09:61|120",
        "2026-11-24|9:00|120", "2026-11-24|09:00:00|120", "2026-11-24|09:15|120",
        "2026-11-28|10:00|120", "2026-11-29|10:00|120", "2026-11-24|07:30|120",
        "2026-11-24|14:30|120", "2026-11-24|15:00|120", "2026-11-24|15:30|60",
        "2026-11-24|23:30|120", "2026-11-24|10:00|90", "2026-1-24|10:00|120",
        "2026-11-24T00:00:00|10:00|120", "2026-11-31|10:00|120"
    })
    void rechazaFechasHorasYDuracionesInvalidas(String fecha,String hora,int duracion){
        assertThrows(IllegalArgumentException.class,()->horario.inicio(fecha,hora,duracion,reloj));
    }
    @Test void aplicaPanamaYTodoElIntervalo(){
        var horas=horario.horas(LocalDate.parse("2026-11-23"),120,reloj);
        assertFalse(horas.contains("10:00"));assertTrue(horas.contains("10:30"));
        assertEquals("14:00",horas.get(horas.size()-1));
        assertTrue(horario.horas(LocalDate.parse("2026-11-24"),60,reloj).contains("15:00"));
        assertEquals(LocalDateTime.parse("2026-11-24T14:00"),horario.inicio("2026-11-24","14:00",120,reloj));
    }
    @Test void sinConfiguracionNoHayConfirmaciones(){
        var h=new HorarioCitas("",30);assertFalse(h.configurado());assertTrue(h.horas(LocalDate.parse("2026-11-24"),120,reloj).isEmpty());
        assertThrows(IllegalStateException.class,()->h.inicio("2026-11-24","10:00",120,reloj));
    }
    @ParameterizedTest @ValueSource(strings={"oficial","8@08:00-16:00","1@16:00-08:00","1@8:00-16:00","1@08:00-24:00","1@08:00-16:00,"})
    void configuracionInvalidaFallaAlIniciar(String valor){assertThrows(IllegalArgumentException.class,()->new HorarioCitas(valor,30));}
    @Test void respetaPausasYConfiguracionDelPaso(){
        var h=new HorarioCitas("2@08:00-12:00,2@13:00-16:00",30);
        assertThrows(IllegalArgumentException.class,()->h.inicio("2026-11-24","11:00",120,reloj));
        assertTrue(h.horas(LocalDate.parse("2026-11-24"),120,reloj).contains("13:00"));
        assertThrows(IllegalArgumentException.class,()->new HorarioCitas("",7));
    }
}
