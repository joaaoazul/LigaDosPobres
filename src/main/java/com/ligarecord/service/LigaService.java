package com.ligarecord.service;

import com.ligarecord.domain.Equipa;
import com.ligarecord.domain.Liga;
import com.ligarecord.domain.enums.EstadoEquipa;
import com.ligarecord.domain.enums.EstadoLiga;
import com.ligarecord.repository.EquipaRepository;
import com.ligarecord.repository.LigaRepository;

import java.util.List;
import java.util.UUID;

public class LigaService {
    private LigaRepository ligaRepository;
    private EquipaRepository equipaRepository;

    public LigaService(LigaRepository ligaRepository, EquipaRepository equipaRepository){
        this.ligaRepository = ligaRepository;
        this.equipaRepository = equipaRepository;
    }

    //esta func vai ver se o nr de equipas e o nome são validos
    public Liga criarLiga(String nome, int maxEquipas){
        if(nome == null ||nome .isBlank()){
            throw new IllegalArgumentException("O nome da liga é obrigatório.");
        }
        if (maxEquipas <= 0 || maxEquipas > 45){
            throw new IllegalArgumentException("O máximo de equipas permitidas na liga é de 45");
        }

        UUID id = UUID.randomUUID();

        Liga liga = new Liga(
                id,
                nome,
                maxEquipas,
                EstadoLiga.ATIVA
        );

        ligaRepository.guardarLiga(liga);

        return liga;
    }

    public Equipa adicionarEquipa(Liga liga, Equipa equipa){

        if(liga == null){
            throw new IllegalArgumentException("A liga é obrigatória.");
        }

        if(equipa == null){
            throw new IllegalArgumentException("A equipa é obrigatória.");
        }

        if(liga.getEstado() != EstadoLiga.ATIVA){
            throw new IllegalStateException(
                    "Não é possível adicionar equipas a uma liga desativada."
            );
        }
        if(liga.getEquipas().size() >= liga.getMaxEquipas()){
            throw new IllegalStateException(
                    "A liga já atingiu o número máximo de equipas."
            );
        }
        if(liga.getEquipas().contains(equipa)){
            throw new IllegalStateException(
                    "A equipa já pertence a esta liga."
            );
        }
        if(equipa.getLiga() != null){
            throw new IllegalStateException(
                    "A equipa já pertence a uma liga."
            );
        }
        liga.adicionarEquipa(equipa);
        equipa.setLiga(liga);
        equipa.setEstado(EstadoEquipa.ATIVA);
        equipaRepository.guardar(equipa);
        ligaRepository.guardarLiga(liga);

        return equipa;

    }

    public Equipa registarDesistencia(Liga liga, Equipa equipa){
        if(liga == null){
            throw new IllegalArgumentException("A liga não existe.");
        }
        if(equipa == null){
            throw new IllegalArgumentException("A equipa não existe");
        }
        if(liga.getEstado() != EstadoLiga.ATIVA){
                throw new IllegalStateException(
                        "Não é possível registar uma desistência numa liga desativada."
                );
            }
        if(equipa.getLiga() != liga){
            throw new IllegalStateException("Esta equipa não pertence a essa liga.");
        }
        if(equipa.getEstado() != EstadoEquipa.ATIVA){
            throw new IllegalStateException("Esta equipa já não se encontra ativa.");
        }

        equipa.setEstado(EstadoEquipa.DESISTENTE);
        equipaRepository.guardar(equipa);
    return equipa;
    }

    public Liga terminarLiga(Liga liga){

        if(liga == null){
            throw new IllegalArgumentException("A liga não existe.");
        }

        if(liga.getEstado() != EstadoLiga.ATIVA){
            throw new IllegalStateException("Esta liga já se encontrava desativada.");
        }

        liga.setEstado(EstadoLiga.DESATIVADA);
        ligaRepository.guardarLiga(liga);
        return liga;
    }
}
