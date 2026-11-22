package com.senniaf.match.service;

import com.senniaf.match.model.*;
import com.senniaf.match.model.enums.*;
import java.util.List;

final class PerfilesCompatibilidad {
    private PerfilesCompatibilidad() {}

    static PerfilNino nino() {
        var n = new PerfilNino();
        n.setCodigoCaso("PRUEBA-N");
        n.setPerfilCompleto(true);
        n.setEdad(10);
        n.setUnidadEdad(UnidadEdad.ANIOS);
        n.setTieneHermanos(SiNo.NO);
        n.setCantidadHermanos(0);
        n.setCondicionesSalud(List.of(CondicionSalud.NINGUNA_CONOCIDA));
        n.setAlergias(List.of(Alergia.NINGUNA_CONOCIDA));
        n.setIntoleranciaLactosa(RespuestaSiNoNoSabe.NO);
        return n;
    }

    static PerfilFamilia familia() {
        var f = new PerfilFamilia();
        f.setCodigoCaso("PRUEBA-F");
        f.setPerfilCompleto(true);
        f.setEdadMinimaAceptada(1);
        f.setEdadMaximaAceptada(17);
        f.setDisposicionHermanos(DisposicionHermanos.DISPUESTA);
        f.setRedApoyo(List.of(FuenteApoyo.PAREJA));
        f.setDisponibilidadViaje(DisponibilidadViaje.ALTA);
        f.setCondicionesPuedeAcompanarHoy(List.of());
        f.setCondicionesConApoyo(List.of());
        return f;
    }
}
