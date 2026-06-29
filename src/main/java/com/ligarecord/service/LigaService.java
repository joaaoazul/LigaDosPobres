package com.ligarecord.service;

import com.ligarecord.domain.Equipa;
import com.ligarecord.domain.Liga;
import com.ligarecord.domain.enums.EstadoEquipa;
import com.ligarecord.domain.enums.EstadoLiga;
import com.ligarecord.repository.LigaRepository;

import java.util.List;

public class LigaService {
    private LigaRepository ligaRepository;

    public LigaService(LigaRepository ligaRepository){
        this.ligaRepository = ligaRepository;
    }

    public Liga criarLiga(String nome, int maxEquipas){
        return null;
    }

    public Equipa adicionarEquipa(Liga liga, Equipa equipa){
        return null;
    }

    public Equipa registarDesistencia(Liga liga, Equipa equipa){
        return null;
    }

    public Liga terminarLiga(Liga liga){
        return null;
    }
}
