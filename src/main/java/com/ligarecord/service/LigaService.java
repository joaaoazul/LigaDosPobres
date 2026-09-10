package com.ligarecord.service;

import com.ligarecord.domain.Equipa;
import com.ligarecord.domain.Gestor;
import com.ligarecord.domain.Liga;
import com.ligarecord.domain.RegraDivida;
import com.ligarecord.domain.enums.EstadoEquipa;
import com.ligarecord.domain.enums.EstadoLiga;
import com.ligarecord.repository.EquipaRepository;
import com.ligarecord.repository.LigaRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class LigaService {
    private LigaRepository ligaRepository;
    private EquipaRepository equipaRepository;
    private final RegraDividaService regraDividaService;
    private final DividaService dividaService;

    public LigaService(LigaRepository ligaRepository,
                       EquipaRepository equipaRepository,
                       RegraDividaService regraDividaService,
                       DividaService dividaService){
        this.ligaRepository = ligaRepository;
        this.equipaRepository = equipaRepository;
        this.regraDividaService = regraDividaService;
        this.dividaService = dividaService;
    }

    @Transactional
    public Liga criarLiga(Gestor gestor, String nome, int maxEquipas){
        if(gestor == null){
            throw new IllegalArgumentException("A liga tem de ter um gestor.");
        }
        String nomeValidado = RegrasDeConta.textoValidado(nome, RegrasDeConta.MAXIMO_NOME, "O nome da liga");
        if (maxEquipas <= 0 || maxEquipas > 45){
            throw new IllegalArgumentException("O máximo de equipas permitidas na liga é de 45");
        }

        UUID id = UUID.randomUUID();

        Liga liga = new Liga(
                id,
                nomeValidado,
                maxEquipas,
                EstadoLiga.ATIVA,
                gestor
        );

        ligaRepository.guardarLiga(liga);

        return liga;
    }

    @Transactional
    public Equipa adicionarEquipa(Liga liga, Equipa equipa){

        if(liga == null){
            throw new IllegalArgumentException("A liga é obrigatória.");
        }

        if(equipa == null){
            throw new IllegalArgumentException("A equipa é obrigatória.");
        }
        equipa.setNome(RegrasDeConta.textoValidado(equipa.getNome(), RegrasDeConta.MAXIMO_NOME,
                "O nome da equipa"));

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
        // Duas equipas com o mesmo nome na mesma liga tornam a classificação e as
        // jornadas ambíguas — não há como saber qual é qual a olhar para a tabela.
        boolean nomeRepetido = liga.getEquipas().stream()
                .anyMatch(existente -> existente.getNome().equalsIgnoreCase(equipa.getNome()));
        if (nomeRepetido) {
            throw new IllegalStateException("Já existe uma equipa com este nome nesta liga.");
        }
        liga.adicionarEquipa(equipa);
        equipa.setLiga(liga);
        equipa.setEstado(EstadoEquipa.ATIVA);
        equipaRepository.guardar(equipa);
        ligaRepository.guardarLiga(liga);

        regraDividaService.buscarPorLiga(liga)
                .map(RegraDivida::getValorInscricao)
                .ifPresent(valorInscricao -> dividaService.registarInscricao(equipa, valorInscricao));

        return equipa;

    }

    /**
     * Corrige o nome de uma equipa já inscrita — gralhas de escrita ou o nome
     * a sério da equipa a mudar de época para época. O gestor faz isto sem
     * precisar do admin: é dono da liga, o nome é dele para gerir.
     */
    @Transactional
    public Equipa alterarNomeEquipa(Liga liga, Equipa equipa, String nome){
        if(liga == null){
            throw new IllegalArgumentException("A liga é obrigatória.");
        }
        if(equipa == null){
            throw new IllegalArgumentException("A equipa é obrigatória.");
        }
        String nomeValidado = RegrasDeConta.textoValidado(nome, RegrasDeConta.MAXIMO_NOME, "O nome da equipa");

        // A mesma regra de adicionarEquipa: duas equipas com o mesmo nome na
        // mesma liga tornam a classificação e as jornadas ambíguas. Exclui a
        // própria equipa, senão renomear sem mudar de nome rebentava sempre.
        boolean nomeRepetido = liga.getEquipas().stream()
                .filter(existente -> !existente.equals(equipa))
                .anyMatch(existente -> existente.getNome().equalsIgnoreCase(nomeValidado));
        if (nomeRepetido) {
            throw new IllegalStateException("Já existe uma equipa com este nome nesta liga.");
        }

        equipa.setNome(nomeValidado);
        equipaRepository.guardar(equipa);
        return equipa;
    }

    @Transactional
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

    @Transactional
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
