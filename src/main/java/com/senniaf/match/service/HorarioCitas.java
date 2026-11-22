package com.senniaf.match.service;

import java.time.*;
import java.time.format.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class HorarioCitas {
    public static final ZoneId ZONA=ZoneId.of("America/Panama");
    private static final DateTimeFormatter FECHA=DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter HORA=DateTimeFormatter.ofPattern("HH:mm").withResolverStyle(ResolverStyle.STRICT);
    private record Franja(DayOfWeek dia,LocalTime desde,LocalTime hasta) {}
    private final List<Franja> franjas;
    private final int paso;
    public HorarioCitas(@Value("${senniaf.citas.franjas:}") String configuracion,
                        @Value("${senniaf.citas.intervalo-minutos:30}") int paso) {
        if(!Set.of(15,30,60).contains(paso))throw new IllegalArgumentException("El intervalo técnico de citas debe ser 15, 30 o 60 minutos");
        this.paso=paso;List<Franja> datos=new ArrayList<>();
        if(configuracion!=null && !configuracion.isBlank()) for(String entrada:configuracion.split(",",-1)) {
            try {
                String[] d=entrada.strip().split("@",-1),h=d[1].split("-",-1);
                if(d.length!=2 || h.length!=2)throw new IllegalArgumentException();
                LocalTime desde=hora(h[0]),hasta=hora(h[1]);
                if(!desde.isBefore(hasta))throw new IllegalArgumentException();
                datos.add(new Franja(DayOfWeek.of(Integer.parseInt(d[0])),desde,hasta));
            } catch(RuntimeException e) {throw new IllegalArgumentException("Configuración de citas inválida; usa día ISO@HH:mm-HH:mm",e);}
        }
        franjas=List.copyOf(datos);
    }
    public boolean configurado(){return !franjas.isEmpty();}
    public int paso(){return paso;}
    public LocalDate fecha(String valor) {
        try {if(valor==null || !valor.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}"))throw new DateTimeException("");return LocalDate.parse(valor,FECHA);}
        catch(DateTimeException e){throw new IllegalArgumentException("Selecciona una fecha válida");}
    }
    private LocalTime hora(String valor) {
        try {if(valor==null || !valor.matches("[0-9]{2}:[0-9]{2}"))throw new DateTimeException("");return LocalTime.parse(valor,HORA);}
        catch(DateTimeException e){throw new IllegalArgumentException("Selecciona una hora válida");}
    }
    public LocalDateTime inicio(String fecha,String hora,int duracion,Clock reloj) {
        if(!Set.of(60,120).contains(duracion))throw new IllegalArgumentException("Selecciona una duración de una o dos horas");
        LocalDateTime inicio=LocalDateTime.of(fecha(fecha),hora(hora));
        if(!inicio.isAfter(LocalDateTime.now(reloj.withZone(ZONA))))throw new IllegalArgumentException("Selecciona una fecha y hora futuras");
        if(!configurado())throw new IllegalStateException("Los horarios de citas todavía no están habilitados");
        if(!permite(inicio,duracion))throw new IllegalArgumentException("El intervalo completo no está disponible en los horarios habilitados");
        return inicio;
    }
    private boolean permite(LocalDateTime inicio,int duracion) {
        var fin=inicio.plusMinutes(duracion);
        return fin.toLocalDate().equals(inicio.toLocalDate()) && franjas.stream().anyMatch(f->f.dia()==inicio.getDayOfWeek()
                && !inicio.toLocalTime().isBefore(f.desde()) && !fin.toLocalTime().isAfter(f.hasta())
                && Duration.between(f.desde(),inicio.toLocalTime()).toMinutes()%paso==0);
    }
    public List<String> horas(LocalDate dia,int duracion,Clock reloj) {
        if(!Set.of(60,120).contains(duracion))throw new IllegalArgumentException("Selecciona una duración de una o dos horas");
        Set<String> horas=new TreeSet<>();
        for(var f:franjas)if(f.dia()==dia.getDayOfWeek())for(LocalTime t=f.desde();!t.plusMinutes(duracion).isAfter(f.hasta()) && t.isBefore(f.hasta());) {
            LocalDateTime inicio=dia.atTime(t);
            if(inicio.isAfter(LocalDateTime.now(reloj.withZone(ZONA))) && permite(inicio,duracion))horas.add(t.format(HORA));
            if(t.plusMinutes(paso).isBefore(t))break;
            t=t.plusMinutes(paso);
        }
        return List.copyOf(horas);
    }
}
