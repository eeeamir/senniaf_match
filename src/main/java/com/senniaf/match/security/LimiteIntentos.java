package com.senniaf.match.security;

import org.springframework.stereotype.Component;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;

/** Ventanas acotadas para una única instancia; no registra identidades ni credenciales. */
@Component
public class LimiteIntentos {
    private record Ventana(long hasta, int usados) {}
    private final Map<String,Ventana> ventanas = new HashMap<>();
    private final Clock reloj;
    public LimiteIntentos() { this(Clock.systemUTC()); }
    LimiteIntentos(Clock reloj) { this.reloj=reloj; }
    public synchronized boolean admitir(String clave, int maximo, long duracionMs) {
        long ahora=reloj.millis();
        Ventana v=ventanas.get(clave);
        if(v==null || v.hasta()<=ahora) {
            if(ventanas.size()>=5000) {
                ventanas.entrySet().removeIf(e->e.getValue().hasta()<=ahora);
                if(ventanas.size()>=5000 && !ventanas.containsKey(clave)) return false;
            }
            v=new Ventana(ahora+duracionMs,0);
        }
        if(v.usados()>=maximo) return false;
        ventanas.put(clave,new Ventana(v.hasta(),v.usados()+1));return true;
    }
    public synchronized void limpiar(String clave) { ventanas.remove(clave); }
}
