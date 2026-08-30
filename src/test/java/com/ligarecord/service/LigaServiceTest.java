package com.ligarecord.service;

import com.ligarecord.domain.Gestor;
import com.ligarecord.domain.Liga;
import com.ligarecord.repository.EquipaRepository;
import com.ligarecord.repository.EquipaRepositoryImpl;
import com.ligarecord.repository.LigaRepository;
import com.ligarecord.repository.LigaRepositoryImpl;
import com.ligarecord.domain.enums.EstadoLiga;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LigaServiceTest {

    private LigaService ligaService;
    private LigaRepository ligaRepository;
    private EquipaRepository equipaRepository;
    private Gestor gestor;

    @BeforeEach
    void setUp() {
        ligaRepository = new LigaRepositoryImpl();
        equipaRepository = new EquipaRepositoryImpl();

        ligaService = new LigaService(
                ligaRepository,
                equipaRepository
        );

        gestor = new Gestor(UUID.randomUUID(), "gestor@teste.pt", "hash", "Gestor de Teste");
    }

    @Test
    void deveCriarLiga() {

        Liga liga = ligaService.criarLiga(gestor, "Liga dos Pobres", 10);

        assertNotNull(liga);
        assertNotNull(liga.getId());

        assertEquals("Liga dos Pobres", liga.getNome());
        assertEquals(10, liga.getMaxEquipas());
        assertEquals(EstadoLiga.ATIVA, liga.getEstado());

        assertEquals(1, ligaRepository.listarLigas(gestor).size());
    }

    @Test
    void naoDeveCriarLigaSemNome() {

        assertThrows(
                IllegalArgumentException.class,
                () -> ligaService.criarLiga(gestor, "", 10)
        );
    }

    @Test
    void naoDeveCriarLigaComMaisDe45Equipas() {

        assertThrows(
                IllegalArgumentException.class,
                () -> ligaService.criarLiga(gestor, "Liga dos Pobres", 46)
        );
    }

    @Test
    void naoDeveCriarLigaComNumeroNegativoDeEquipas() {

        assertThrows(
                IllegalArgumentException.class,
                () -> ligaService.criarLiga(gestor, "Liga dos Pobres", -1)
        );
    }

    @Test
    void deveTerminarLiga() {

        Liga liga = ligaService.criarLiga(gestor, "Liga dos Pobres", 10);

        Liga resultado = ligaService.terminarLiga(liga);

        assertEquals(EstadoLiga.DESATIVADA, resultado.getEstado());

        assertEquals(
                EstadoLiga.DESATIVADA,
                ligaRepository.listarLigas(gestor).get(0).getEstado()
        );
    }

    @Test
    void naoDeveTerminarLigaDuasVezes() {

        Liga liga = ligaService.criarLiga(gestor, "Liga dos Pobres", 10);

        ligaService.terminarLiga(liga);

        assertThrows(
                IllegalStateException.class,
                () -> ligaService.terminarLiga(liga)
        );
    }

    @Test
    void naoDeveTerminarLigaInexistente() {

        assertThrows(
                IllegalArgumentException.class,
                () -> ligaService.terminarLiga(null)
        );
    }
}