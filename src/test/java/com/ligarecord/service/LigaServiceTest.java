package com.ligarecord.service;

import com.ligarecord.domain.Equipa;
import com.ligarecord.domain.Gestor;
import com.ligarecord.domain.Liga;
import com.ligarecord.domain.Treinador;
import com.ligarecord.repository.DividaRepository;
import com.ligarecord.repository.DividaRepositoryImpl;
import com.ligarecord.repository.EquipaRepository;
import com.ligarecord.repository.EquipaRepositoryImpl;
import com.ligarecord.repository.LigaRepository;
import com.ligarecord.repository.LigaRepositoryImpl;
import com.ligarecord.repository.RegraDividaRepository;
import com.ligarecord.repository.RegraDividaRepositoryImpl;
import com.ligarecord.domain.enums.EstadoEquipa;
import com.ligarecord.domain.enums.EstadoLiga;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LigaServiceTest {

    private LigaService ligaService;
    private LigaRepository ligaRepository;
    private EquipaRepository equipaRepository;
    private RegraDividaRepository regraDividaRepository;
    private RegraDividaService regraDividaService;
    private DividaService dividaService;
    private Gestor gestor;

    @BeforeEach
    void setUp() {
        ligaRepository = new LigaRepositoryImpl();
        equipaRepository = new EquipaRepositoryImpl();
        regraDividaRepository = new RegraDividaRepositoryImpl();
        regraDividaService = new RegraDividaService(regraDividaRepository);
        DividaRepository dividaRepository = new DividaRepositoryImpl();
        dividaService = new DividaService(dividaRepository, new ClassificacaoService());

        ligaService = new LigaService(
                ligaRepository,
                equipaRepository,
                regraDividaService,
                dividaService
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

    @Test
    void cobraAInscricaoQuandoALigaTemRegraDeDivida() {
        Liga liga = ligaService.criarLiga(gestor, "Liga dos Pobres", 10);
        regraDividaService.definir(liga, new BigDecimal("5.00"), BigDecimal.ZERO,
                new BigDecimal("0.50"), 5, new BigDecimal("2.50"), 5);

        Equipa equipa = new Equipa(UUID.randomUUID(), "Equipa de Teste",
                new Treinador(UUID.randomUUID(), "João Azul"), null, EstadoEquipa.ATIVA);
        ligaService.adicionarEquipa(liga, equipa);

        var divida = dividaService.buscarPorEquipa(equipa).orElseThrow();
        assertEquals(new BigDecimal("5.00"), dividaService.calcularTotalDivida(divida));
    }

    @Test
    void naoCobraInscricaoSemRegraDeDivida() {
        Liga liga = ligaService.criarLiga(gestor, "Liga dos Pobres", 10);

        Equipa equipa = new Equipa(UUID.randomUUID(), "Equipa de Teste",
                new Treinador(UUID.randomUUID(), "João Azul"), null, EstadoEquipa.ATIVA);
        ligaService.adicionarEquipa(liga, equipa);

        assertTrue(dividaService.buscarPorEquipa(equipa).isEmpty());
    }

    @Test
    void deveAlterarNomeDaEquipa() {
        Liga liga = ligaService.criarLiga(gestor, "Liga dos Pobres", 10);
        Equipa equipa = new Equipa(UUID.randomUUID(), "Equipa de Teste",
                new Treinador(UUID.randomUUID(), "João Azul"), null, EstadoEquipa.ATIVA);
        ligaService.adicionarEquipa(liga, equipa);

        Equipa resultado = ligaService.alterarNomeEquipa(liga, equipa, "Equipa Renomeada");

        assertEquals("Equipa Renomeada", resultado.getNome());
        assertEquals("Equipa Renomeada", equipa.getNome());
    }

    @Test
    void deveAceitarRenomearParaOMesmoNome() {
        Liga liga = ligaService.criarLiga(gestor, "Liga dos Pobres", 10);
        Equipa equipa = new Equipa(UUID.randomUUID(), "Equipa de Teste",
                new Treinador(UUID.randomUUID(), "João Azul"), null, EstadoEquipa.ATIVA);
        ligaService.adicionarEquipa(liga, equipa);

        Equipa resultado = ligaService.alterarNomeEquipa(liga, equipa, "Equipa de Teste");

        assertEquals("Equipa de Teste", resultado.getNome());
    }

    @Test
    void naoDeveAlterarNomeDaEquipaParaVazio() {
        Liga liga = ligaService.criarLiga(gestor, "Liga dos Pobres", 10);
        Equipa equipa = new Equipa(UUID.randomUUID(), "Equipa de Teste",
                new Treinador(UUID.randomUUID(), "João Azul"), null, EstadoEquipa.ATIVA);
        ligaService.adicionarEquipa(liga, equipa);

        assertThrows(
                IllegalArgumentException.class,
                () -> ligaService.alterarNomeEquipa(liga, equipa, "")
        );
    }

    @Test
    void naoDeveAlterarNomeDaEquipaParaNomeJaUsadoNaLiga() {
        Liga liga = ligaService.criarLiga(gestor, "Liga dos Pobres", 10);
        Equipa equipaA = new Equipa(UUID.randomUUID(), "Equipa A",
                new Treinador(UUID.randomUUID(), "João Azul"), null, EstadoEquipa.ATIVA);
        Equipa equipaB = new Equipa(UUID.randomUUID(), "Equipa B",
                new Treinador(UUID.randomUUID(), "Outro Treinador"), null, EstadoEquipa.ATIVA);
        ligaService.adicionarEquipa(liga, equipaA);
        ligaService.adicionarEquipa(liga, equipaB);

        assertThrows(
                IllegalStateException.class,
                () -> ligaService.alterarNomeEquipa(liga, equipaB, "equipa a")
        );
    }
}