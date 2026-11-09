package com.senniaf.match.web;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.time.*;

/** Expone la zona del servidor de origen sin reescribir fechas almacenadas. */
public class FechaConZonaSerializer extends JsonSerializer<LocalDateTime> {
    @Override public void serialize(LocalDateTime fecha,JsonGenerator salida,SerializerProvider provider) throws IOException {
        salida.writeString(fecha.atZone(ZoneId.systemDefault()).toOffsetDateTime().toString());
    }
}
