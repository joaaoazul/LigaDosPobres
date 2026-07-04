package com.ligarecord.service;

import com.ligarecord.domain.Desempate;
import com.ligarecord.domain.Equipa;
import com.ligarecord.domain.Jornada;
import com.ligarecord.repository.DesempateRepository;

import java.util.List;

public class DesempateService {

    private DesempateRepository desempateRepository;

    public DesempateService(DesempateRepository desempateRepository){
        this.desempateRepository = desempateRepository;
    }

    public List<Equipa> detetarEmpates(Jornada jornada){
        return null;
    }

    public boolean temEmpatesPorResolver(Jornada jornada){
        return false;
    }

    public Desempate resolverDesempate(Desempate desempate, List<Equipa> ordemAdmin){
        return null;
    }
}
