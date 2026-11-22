package com.senniaf.match;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprobaciones de solo lectura contra el despliegue público en Azure (HTTPS).
 * Solo envía GET a páginas públicas y a un endpoint protegido sin sesión; no crea ni modifica datos.
 * Se activa con la variable de entorno SENNIAF_AZURE_URL, por ejemplo:
 *   $env:SENNIAF_AZURE_URL="https://senniafmatch.westus2.cloudapp.azure.com"
 */
@EnabledIfEnvironmentVariable(named = "SENNIAF_AZURE_URL", matches = "https://.+")
class AzurePublicoSprint9IT {

    static final HttpClient CLIENTE = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    static HttpResponse<String> get(String ruta) throws Exception {
        String base = System.getenv("SENNIAF_AZURE_URL").replaceAll("/+$", "");
        return CLIENTE.send(HttpRequest.newBuilder(URI.create(base + ruta)).timeout(Duration.ofSeconds(30)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void AZ01_portadaPublicaRespondeConEnlaceConocerElSistema() throws Exception {
        var r = get("/index.html");
        assertEquals(200, r.statusCode());
        assertTrue(r.body().contains("Conocer el sistema"));
    }

    @Test
    void AZ02_paginaConocerElSistemaSeAbreSinSesion() throws Exception {
        assertEquals(200, get("/conocer-sistema.html").statusCode());
    }

    @Test
    void AZ03_apiProtegidaRechazaPeticionSinSesion() throws Exception {
        int codigo = get("/api/citas").statusCode();
        assertTrue(codigo >= 400 && codigo < 500, "Código " + codigo);
    }

    @Test
    void AZ04_respuestasSeSirvenPorHttps() throws Exception {
        assertTrue(System.getenv("SENNIAF_AZURE_URL").startsWith("https://"));
        assertEquals("https", get("/index.html").uri().getScheme());
    }
}
