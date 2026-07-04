package com.ligarecord.service;

import com.ligarecord.domain.*;
import com.ligarecord.repository.DividaRepository;
import com.ligarecord.repository.EquipaRepository;

import java.math.BigDecimal;
import java.util.Map;

public class DividaService {

    private DividaRepository dividaRepository;

    private EquipaRepository equipaRepository;

    public DividaService(DividaRepository dividaRepository, EquipaRepository equipaRepository){
        this.dividaRepository = dividaRepository;
        this.equipaRepository = equipaRepository;
    }

    public boolean jornadaFechaBloco(Jornada jornada){
        return false;
    }

    public Map<Equipa, BigDecimal> processarFechoBloco(Liga liga){
        return null;
    }

    public BigDecimal calcularTotalDivida (Divida divida) {
        return null;
    }

    public BigDecimal calcularTotalTreinador(Treinador treinador) {
        return null;
    }

    public Divida resolverDivida(Divida divida){
        return null;
    }

}
