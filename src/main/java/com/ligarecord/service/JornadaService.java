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

    public Jornada abrirJornada(Liga liga, Jornada jornada){
        if (verificaSeTreino(jornada)){

        };
        if (liga == null){
            throw new IllegalArgumentException("Não existe uma liga disponível");
        }

        int countTreino = 0;
        int countOficial = 0;
        for(int i = 0; i < liga.getJornadas().size(); i++){
            if(liga.getJornadas().get(i).iseTreino()){
                countTreino++;
            } else {
                countOficial++;
            }
        }


int jornadaAtual;

        if(countTreino < 5){
            jornadaAtual = countTreino +1;
        }

        if(countTreino >= 5){

        }

    }

    public ResultadoJornada inserirResultado(Jornada jornada, Equipa equipa, int pontuacao){
        return null;
    }

    public Jornada fecharJornada(Jornada jornada){
        return null;
    }

    public boolean verificaSeTreino(Jornada jornada){
        if(jornada == null){
            throw new IllegalArgumentException("Não existe uma jornada válida.");
        } else return jornada.iseTreino();
    }
}
