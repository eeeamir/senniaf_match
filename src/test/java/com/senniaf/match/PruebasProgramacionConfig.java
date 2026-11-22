package com.senniaf.match;
import java.time.*;
@org.springframework.boot.test.context.TestConfiguration
public class PruebasProgramacionConfig {
 @org.springframework.context.annotation.Bean @org.springframework.context.annotation.Primary Reloj relojDePruebas(){return new Reloj();}
 public static class Reloj extends Clock {
  private final java.util.concurrent.atomic.AtomicReference<Instant> ahora=new java.util.concurrent.atomic.AtomicReference<>(Instant.parse("2035-11-14T14:00:00Z"));
  public ZoneId getZone(){return ZoneOffset.UTC;} public Clock withZone(ZoneId z){return Clock.fixed(instant(),z);}public Instant instant(){return ahora.get();}public void avanzar(Instant i){ahora.updateAndGet(a->i.isAfter(a)?i:a);}public void dias(long d){ahora.updateAndGet(a->a.plus(Duration.ofDays(d)));}
 }
}
