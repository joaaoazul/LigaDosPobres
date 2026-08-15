package com.ligarecord.service;

import com.ligarecord.domain.Equipa;
import com.ligarecord.domain.Liga;
import com.ligarecord.domain.Treinador;
import com.ligarecord.domain.enums.EstadoEquipa;
import com.ligarecord.repository.EquipaRepository;
import com.ligarecord.repository.EquipaRepositoryImpl;
import com.ligarecord.repository.LigaRepository;
import com.ligarecord.repository.LigaRepositoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LigaServiceEquipaTest {

    private LigaService ligaService;
    private LigaRepository ligaRepository;
    private EquipaRepository equipaRepository;

    @BeforeEach
    void setUp() {
        ligaRepository = new LigaRepositoryImpl();
        equipaRepository = new EquipaRepositoryImpl();

        ligaService = new LigaService(
                ligaRepository,
                equipaRepository
        );
    }

    private Treinador criarTreinador(String nome) {
        return new Treinador(
                UUID.randomUUID(),
                nome
        );
    }

    private Equipa criarEquipa(String nome, Treinador treinador) {
        return new Equipa(
                UUID.randomUUID(),
                nome,
                treinador,
                null,
                EstadoEquipa.ATIVA
        );
    }

    @Test
    void deveAdicionarEquipa() {

        Liga liga = ligaService.criarLiga("Liga dos Pobres", 10);

        Treinador treinador = criarTreinador("João");
        Equipa equipa = criarEquipa("Os Pobres", treinador);

        Equipa resultado = ligaService.adicionarEquipa(liga, equipa);

        assertEquals(equipa, resultado);

        assertEquals(1, liga.getEquipas().size());
        assertTrue(liga.getEquipas().contains(equipa));

        assertEquals(liga, equipa.getLiga());
        assertEquals(EstadoEquipa.ATIVA, equipa.getEstado());

        assertEquals(1, equipaRepository.buscarPorTreinador(treinador).size());
        assertEquals(1, ligaRepository.listarLigas().get(0).getEquipas().size());
    }

    @Test
    void naoDeveAdicionarEquipaAposLigaTerminada() {

        Liga liga = ligaService.criarLiga("Liga dos Pobres", 10);
        ligaService.terminarLiga(liga);

        Treinador treinador = criarTreinador("João");
        Equipa equipa = criarEquipa("Os Pobres", treinador);

        assertThrows(
                IllegalStateException.class,
                () -> ligaService.adicionarEquipa(liga, equipa)
        );
    }

    @Test
    void naoDeveAdicionarMaisEquipasQueOMaximo() {

        Liga liga = ligaService.criarLiga("Liga dos Pobres", 1);

        Treinador treinador1 = criarTreinador("João");
        Treinador treinador2 = criarTreinador("Pedro");

        Equipa equipa1 = criarEquipa("Equipa 1", treinador1);
        Equipa equipa2 = criarEquipa("Equipa 2", treinador2);

        ligaService.adicionarEquipa(liga, equipa1);

        assertThrows(
                IllegalStateException.class,
                () -> ligaService.adicionarEquipa(liga, equipa2)
        );
    }

    @Test
    void naoDeveAdicionarEquipaQueJaPertenceAOutraLiga() {

        Liga liga1 = ligaService.criarLiga("Liga 1", 10);
        Liga liga2 = ligaService.criarLiga("Liga 2", 10);

        Treinador treinador = criarTreinador("João");
        Equipa equipa = criarEquipa("Os Pobres", treinador);

        ligaService.adicionarEquipa(liga1, equipa);

        assertThrows(
                IllegalStateException.class,
                () -> ligaService.adicionarEquipa(liga2, equipa)
        );
    }

    @Test
    void naoDeveAdicionarAmesmaEquipaDuasVezes() {

        Liga liga = ligaService.criarLiga("Liga dos Pobres", 10);

        Treinador treinador = criarTreinador("João");
        Equipa equipa = criarEquipa("Os Pobres", treinador);

        ligaService.adicionarEquipa(liga, equipa);

        assertThrows(
                IllegalStateException.class,
                () -> ligaService.adicionarEquipa(liga, equipa)
        );
    }

    @Test
    void deveRegistarDesistencia() {

        Liga liga = ligaService.criarLiga("Liga dos Pobres", 10);

        Treinador treinador = criarTreinador("João");
        Equipa equipa = criarEquipa("Os Pobres", treinador);

        ligaService.adicionarEquipa(liga, equipa);

        Equipa resultado = ligaService.registarDesistencia(liga, equipa);

        assertEquals(equipa, resultado);
        assertEquals(EstadoEquipa.DESISTENTE, equipa.getEstado());

        assertEquals(
                EstadoEquipa.DESISTENTE,
                equipaRepository
                        .buscarPorTreinador(treinador)
                        .get(0)
                        .getEstado()
        );
    }

    @Test
    void naoDeveRegistarDesistenciaDuasVezes() {

        Liga liga = ligaService.criarLiga("Liga dos Pobres", 10);

        Treinador treinador = criarTreinador("João");
        Equipa equipa = criarEquipa("Os Pobres", treinador);

        ligaService.adicionarEquipa(liga, equipa);

        ligaService.registarDesistencia(liga, equipa);

        assertThrows(
                IllegalStateException.class,
                () -> ligaService.registarDesistencia(liga, equipa)
        );
    }

    @Test
    void naoDeveRegistarDesistenciaNumaLigaTerminada() {

        Liga liga = ligaService.criarLiga("Liga dos Pobres", 10);

        Treinador treinador = criarTreinador("João");
        Equipa equipa = criarEquipa("Os Pobres", treinador);

        ligaService.adicionarEquipa(liga, equipa);
        ligaService.terminarLiga(liga);

        assertThrows(
                IllegalStateException.class,
                () -> ligaService.registarDesistencia(liga, equipa)
        );
    }
}