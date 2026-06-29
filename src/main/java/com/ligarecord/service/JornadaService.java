package com.ligarecord.service;

import com.ligarecord.domain.Equipa;
import com.ligarecord.domain.Jornada;
import com.ligarecord.domain.Liga;
import com.ligarecord.domain.ResultadoJornada;
import com.ligarecord.repository.JornadaRepository;

public class JornadaService {

    private JornadaRepository jornadaRepository;

    public JornadaService(JornadaRepository jornadaRepository){
        this.jornadaRepository = jornadaRepository;
    }

    public Jornada abrirJornada(Liga liga){
        return null;
    }

    public ResultadoJornada inserirResultado(Jornada jornada, Equipa equipa, int pontuacao){
        return null;
    }

    public Jornada fecharJornada(Jornada jornada){
        return null;
    }

    public boolean verificaSeTreino(Jornada jornada){
        return false;
    }
}
