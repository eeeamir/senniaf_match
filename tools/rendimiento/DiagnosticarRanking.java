package com.senniaf.match.service;

import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import jdk.jfr.Recording;
import jdk.jfr.consumer.*;
import static org.mockito.Mockito.*;

/** Instrumentación separada: estos tiempos no se mezclan con las mediciones del ranking. */
public class DiagnosticarRanking {
    public static void main(String[] args) throws Exception {
        Path salida=Path.of(args[0]);var s=new MedirRanking(100);
        var analizador=spy(new AnalizadorTextoCompatibilidad());
        MedirRanking.campo(s.servicio,"analizador",analizador);
        MedirRanking.LLAMADAS.get().clear();s.servicio.casos();
        verify(analizador,times(1)).necesidades(s.n,s.informes);
        verify(analizador,times(100)).capacidades(any());
        var conteo=new TreeMap<>(MedirRanking.LLAMADAS.get());
        MedirRanking.campo(s.servicio,"analizador",new AnalizadorTextoCompatibilidad());
        for(int i=0;i<20;i++)s.servicio.casos();
        Map<String,Integer> muestras=new TreeMap<>();int total=0;
        Path jfr=salida.resolve("perfil-regex.jfr");
        try(var r=new Recording()) {
            r.enable("jdk.ExecutionSample").withPeriod(Duration.ofMillis(2)).withStackTrace();
            r.start();for(int i=0;i<100;i++)MedirRanking.sumidero=s.servicio.casos();r.stop();r.dump(jfr);
        }
        try(var file=new RecordingFile(jfr)) {
            while(file.hasMoreEvents()) {
                var event=file.readEvent();if(!event.getEventType().getName().equals("jdk.ExecutionSample"))continue;
                total++;var stack=event.getStackTrace();if(stack==null)continue;
                Set<String> unicas=new HashSet<>();
                for(var frame:stack.getFrames()) {
                    String method=frame.getMethod().getType().getName()+"."+frame.getMethod().getName();
                    if(method.equals("java.util.regex.Pattern.compile") || method.equals("java.lang.String.matches")
                            || method.equals("java.lang.String.replaceAll") || method.contains("AnalizadorTextoCompatibilidad.normalizar"))
                        unicas.add(method);
                }
                unicas.forEach(m->muestras.merge(m,1,Integer::sum));
            }
        }
        Files.writeString(salida.resolve("diagnostico.json"),MedirRanking.JSON.writeValueAsString(Map.of(
                "familias",100,"necesidadesPorRanking",1,"capacidadesPorRanking",100,"repositorios",conteo,
                "muestrasEjecucionJfr",total,"muestrasConMetodo",muestras)));
    }
}
