package com.senniaf.match.config;
@org.springframework.context.annotation.Configuration
@org.springframework.scheduling.annotation.EnableScheduling
public class RelojConfiguracion {
 @org.springframework.context.annotation.Bean public java.time.Clock relojInstitucional(){return java.time.Clock.systemUTC();}
}
