package com.ligarecord.service;

import com.ligarecord.domain.BlocoDivida;
import com.ligarecord.domain.Equipa;
import com.ligarecord.domain.Gestor;
import com.ligarecord.domain.Liga;
import com.ligarecord.domain.PoteDaLiga;
import com.ligarecord.domain.Treinador;
import com.ligarecord.domain.enums.EstadoEquipa;
import com.ligarecord.domain.enums.EstadoLiga;
import com.ligarecord.repository.DividaRepository;
import com.ligarecord.repository.DividaRepositoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * O pote da liga: o que foi lançado, o que está dado como pago, o que falta.
 * Somado bloco a bloco — o estado da dívida de uma equipa diz só se ainda
 * sobra alguma coisa, não quanto.
 */
class DividaServicePoteTest {

    private DividaRepository dividaRepository;
    private DividaService dividaService;
    private Gestor gestor;
    private Liga liga;
    private Equipa alfa;
    private Equipa beta;

    @BeforeEach
    void setUp() {
        dividaRepository = new DividaRepositoryImpl();
        dividaService = new DividaService(dividaRepository, new ClassificacaoService());

        gestor = new Gestor(UUID.randomUUID(), "gestor@teste.pt", "hash", "Gestor");
        liga = new Liga(UUID.randomUUID(), "Liga de Teste", 10, EstadoLiga.ATIVA, gestor);
        alfa = equipa("Alfa");
        beta = equipa("Beta");
    }

    private Equipa equipa(String nome) {
        Equipa equipa = new Equipa(UUID.randomUUID(), nome, new Treinador(UUID.randomUUID(), nome), liga,
                EstadoEquipa.ATIVA);
        liga.adicionarEquipa(equipa);
        return equipa;
    }

    @Test
    void ligaSemDividasNenhumasDaTresZeros() {
        PoteDaLiga pote = dividaService.calcularPoteDaLiga(liga);

        assertEquals(BigDecimal.ZERO, pote.total());
        assertEquals(BigDecimal.ZERO, pote.pago());
        assertEquals(BigDecimal.ZERO, pote.porPagar());
    }

    @Test
    void somaInscricoesEBlocosDePeriodoNoMesmoPote() {
        dividaService.registarInscricao(alfa, new BigDecimal("15.00"));
        dividaService.registarBloco(alfa, new BigDecimal("0.50"));
        dividaService.registarInscricao(beta, new BigDecimal("15.00"));

        PoteDaLiga pote = dividaService.calcularPoteDaLiga(liga);

        assertEquals(new BigDecimal("30.50"), pote.total());
        assertEquals(new BigDecimal("30.50"), pote.porPagar());
    }

    /**
     * O caso que obriga a somar bloco a bloco: a dívida da Alfa continua
     * PENDENTE por causa do segundo bloco, mas o primeiro já está pago. Somar
     * pelo estado da dívida punha os 15.00 no lado errado.
     */
    @Test
    void separaPagoDePorPagarDentroDaMesmaDivida() {
        BlocoDivida inscricao = dividaService.registarInscricao(alfa, new BigDecimal("15.00"));
        dividaService.registarBloco(alfa, new BigDecimal("2.50"));
        dividaService.resolverBloco(alfa, inscricao.getId());

        PoteDaLiga pote = dividaService.calcularPoteDaLiga(liga);

        assertEquals(new BigDecimal("17.50"), pote.total());
        assertEquals(new BigDecimal("15.00"), pote.pago());
        assertEquals(new BigDecimal("2.50"), pote.porPagar());
    }

    /** O que uma equipa que desistiu deixou por pagar continua a fazer falta ao pote. */
    @Test
    void contaAsDividasDeEquipasDesistentes() {
        dividaService.registarInscricao(alfa, new BigDecimal("15.00"));
        dividaService.registarInscricao(beta, new BigDecimal("15.00"));
        beta.setEstado(EstadoEquipa.DESISTENTE);

        PoteDaLiga pote = dividaService.calcularPoteDaLiga(liga);

        assertEquals(new BigDecimal("30.00"), pote.total());
        assertEquals(new BigDecimal("30.00"), pote.porPagar());
    }

    @Test
    void naoContaODinheiroDeOutraLiga() {
        Liga outra = new Liga(UUID.randomUUID(), "Outra Liga", 10, EstadoLiga.ATIVA, gestor);
        Equipa deOutraLiga = new Equipa(UUID.randomUUID(), "Gama", new Treinador(UUID.randomUUID(), "Gama"),
                outra, EstadoEquipa.ATIVA);
        outra.adicionarEquipa(deOutraLiga);

        dividaService.registarInscricao(alfa, new BigDecimal("15.00"));
        dividaService.registarInscricao(deOutraLiga, new BigDecimal("99.00"));

        assertEquals(new BigDecimal("15.00"), dividaService.calcularPoteDaLiga(liga).total());
        assertEquals(new BigDecimal("99.00"), dividaService.calcularPoteDaLiga(outra).total());
    }

    /** O total é sempre a soma dos outros dois, por construção. */
    @Test
    void oTotalEsempreAsomaDosOutrosDois() {
        BlocoDivida pago = dividaService.registarInscricao(alfa, new BigDecimal("15.00"));
        dividaService.registarBloco(alfa, new BigDecimal("1.50"));
        dividaService.registarBloco(beta, new BigDecimal("2.50"));
        dividaService.resolverBloco(alfa, pago.getId());

        PoteDaLiga pote = dividaService.calcularPoteDaLiga(liga);

        assertEquals(pote.total(), pote.pago().add(pote.porPagar()));
    }

    /** Um bloco de 0.00€ nasce resolvido, por isso entra no pago sem mexer nas contas. */
    @Test
    void blocoDeValorZeroNaoDesequilibraOPote() {
        dividaService.registarBloco(alfa, BigDecimal.ZERO);
        dividaService.registarBloco(beta, new BigDecimal("0.50"));

        PoteDaLiga pote = dividaService.calcularPoteDaLiga(liga);

        assertEquals(new BigDecimal("0.50"), pote.total());
        assertEquals(new BigDecimal("0.50"), pote.porPagar());
        assertEquals(0, pote.pago().compareTo(BigDecimal.ZERO));
    }
}
