package com.senniaf.match.dto;

import jakarta.validation.constraints.Size;

/** Cinco textos cualitativos independientes; ningún valor interviene en el Match. */
public record ContinuidadCulturalRequest(
    @Size(max=10000,message="Idioma y comunicación admite hasta 10000 caracteres") @com.fasterxml.jackson.annotation.JsonProperty(value="idiomaComunicacion",required=true) String idiomaComunicacion,
    @Size(max=10000,message="Costumbres y tradiciones admite hasta 10000 caracteres") @com.fasterxml.jackson.annotation.JsonProperty(value="costumbresTradiciones",required=true) String costumbresTradiciones,
    @Size(max=10000,message="Vínculos culturales admite hasta 10000 caracteres") @com.fasterxml.jackson.annotation.JsonProperty(value="vinculosCulturales",required=true) String vinculosCulturales,
    @Size(max=10000,message="Preferencias y opinión admite hasta 10000 caracteres") @com.fasterxml.jackson.annotation.JsonProperty(value="opinionCultural",required=true) String opinionCultural,
    @Size(max=10000,message="Recomendaciones culturales admite hasta 10000 caracteres") @com.fasterxml.jackson.annotation.JsonProperty(value="recomendacionesCulturales",required=true) String recomendacionesCulturales
) {}
