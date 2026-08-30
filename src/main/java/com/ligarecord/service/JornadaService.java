package com.ligarecord.service;

import com.ligarecord.domain.Equipa;
import com.ligarecord.domain.Jornada;
import com.ligarecord.domain.Liga;
import com.ligarecord.domain.ResultadoJornada;
import com.ligarecord.domain.enums.EstadoEquipa;
import com.ligarecord.domain.enums.EstadoJornada;
import com.ligarecord.domain.enums.EstadoJornadaTreino;
import com.ligarecord.domain.enums.EstadoLiga;
import com.ligarecord.repository.JornadaRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class JornadaService {

    private static final int NUMERO_JORNADAS_TREINO = 5;

    private JornadaRepository jornadaRepository;

    public JornadaService(JornadaRepository jornadaRepository){
        this.jornadaRepository = jornadaRepository;
    }

    /**
     * Abre a próxima jornada da liga. As primeiras {@value #NUMERO_JORNADAS_TREINO}
     * jornadas são de treino, as seguintes são oficiais. Cada tipo tem a sua
     * própria numeração.
     */
    @Transactional
    public Jornada abrirJornada(Liga liga){
        if (liga == null){
            throw new IllegalArgumentException("Não existe uma liga disponível");
        }

        if (liga.getEstado() != EstadoLiga.ATIVA){
            throw new IllegalStateException("Não é possível abrir jornadas numa liga desativada.");
        }

        if (existeJornadaAberta(liga)){
            throw new IllegalStateException("Já existe uma jornada aberta nesta liga.");
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
        EstadoJornadaTreino tipoJornada;

        if(countTreino < NUMERO_JORNADAS_TREINO){
            tipoJornada = EstadoJornadaTreino.TREINO;
            jornadaAtual = countTreino + 1;
        } else {
            tipoJornada = EstadoJornadaTreino.OFICIAL;
            jornadaAtual = countOficial + 1;
        }

        Jornada jornada = new Jornada(
                UUID.randomUUID(),
                jornadaAtual,
                EstadoJornada.ABERTA,
                tipoJornada,
                liga
        );

        liga.adicionarJornada(jornada);
        jornadaRepository.guardar(jornada);

        return jornada;
    }

    @Transactional
    public ResultadoJornada inserirResultado(Jornada jornada, Equipa equipa, int pontuacao){
        if(jornada == null){
            throw new IllegalArgumentException("Não existe uma jornada válida.");
        }
        if(equipa == null){
            throw new IllegalArgumentException("A equipa é obrigatória.");
        }
        if(jornada.getEstadoJ() == EstadoJornada.FECHADA){
            throw new IllegalStateException("A jornada já está fechada.");
        }
        if(equipa.getEstado() != EstadoEquipa.ATIVA){
            throw new IllegalStateException("Não é possível inserir resultados de uma equipa desistente.");
        }
        if(pontuacao < 0){
            throw new IllegalArgumentException("A pontuação não pode ser negativa.");
        }

        for(ResultadoJornada existente : jornada.getResultadoJ()){
            if(existente.getEquipa().getId().equals(equipa.getId())){
                existente.setPontuacao(pontuacao);
                jornadaRepository.guardar(jornada);
                return existente;
            }
        }

        ResultadoJornada resultado = new ResultadoJornada(
                UUID.randomUUID(),
                equipa,
                jornada,
                pontuacao,
                0,
                false
        );

        jornada.adicionarResultado(resultado);
        jornadaRepository.guardar(jornada);

        return resultado;
    }

    /**
     * Fecha a jornada e atribui as posições por ordem decrescente de pontuação.
     * Equipas empatadas ficam, para já, com a mesma posição — a resolução de
     * empates é feita pelo {@link DesempateService}.
     */
    @Transactional
    public Jornada fecharJornada(Jornada jornada){
        if(jornada == null){
            throw new IllegalArgumentException("Não existe uma jornada válida.");
        }
        if(jornada.getEstadoJ() == EstadoJornada.FECHADA){
            throw new IllegalStateException("Esta jornada já se encontra fechada.");
        }
        if(jornada.getResultadoJ().isEmpty()){
            throw new IllegalStateException("Não é possível fechar uma jornada sem resultados.");
        }

        List<ResultadoJornada> ordenados = new ArrayList<>(jornada.getResultadoJ());
        ordenados.sort(Comparator.comparingInt(ResultadoJornada::getPontuacao).reversed());

        int posicao = 0;
        Integer pontuacaoAnterior = null;
        for(int i = 0; i < ordenados.size(); i++){
            ResultadoJornada resultado = ordenados.get(i);
            if(pontuacaoAnterior == null || resultado.getPontuacao() != pontuacaoAnterior){
                posicao = i + 1;
                pontuacaoAnterior = resultado.getPontuacao();
            }
            resultado.setPosicao(posicao);
        }

        jornada.setEstadoJ(EstadoJornada.FECHADA);
        jornadaRepository.guardar(jornada);

        return jornada;
    }

    public boolean verificaSeTreino(Jornada jornada){
        if(jornada == null){
            throw new IllegalArgumentException("Não existe uma jornada válida.");
        } else return jornada.iseTreino();
    }

    private boolean existeJornadaAberta(Liga liga){
        for(Jornada jornada : liga.getJornadas()){
            if(jornada.getEstadoJ() != EstadoJornada.FECHADA){
                return true;
            }
        }
        return false;
    }
}
