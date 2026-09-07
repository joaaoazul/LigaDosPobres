package com.ligarecord.service;

import com.ligarecord.domain.Divida;
import com.ligarecord.domain.Equipa;
import com.ligarecord.domain.Gestor;
import com.ligarecord.domain.Jornada;
import com.ligarecord.domain.Liga;
import com.ligarecord.domain.ResultadoJornada;
import com.ligarecord.domain.Treinador;
import com.ligarecord.domain.enums.EstadoEquipa;
import com.ligarecord.domain.enums.EstadoJornada;
import com.ligarecord.domain.enums.EstadoLiga;
import com.ligarecord.repository.DividaRepository;
import com.ligarecord.repository.DividaRepositoryImpl;
import com.ligarecord.repository.JornadaRepository;
import com.ligarecord.repository.JornadaRepositoryImpl;
import com.ligarecord.repository.RegraDividaRepository;
import com.ligarecord.repository.RegraDividaRepositoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Desempate manual de uma jornada. O que está aqui em jogo não é só a ordem da
 * tabela: o valor de cada bloco de dívida sai da posição de cada equipa na
 * jornada, por isso um empate por desfazer é dinheiro cobrado a menos a umas
 * equipas e a mais a outras.
 */
class JornadaServiceDesempateTest {

    private JornadaService jornadaService;
    private DividaService dividaService;
    private RegraDividaService regraDividaService;
    private Liga liga;
    private Equipa primeira;
    private Equipa segunda;
    private Equipa terceira;
    private Equipa quarta;

    @BeforeEach
    void setUp() {
        JornadaRepository jornadaRepository = new JornadaRepositoryImpl();
        RegraDividaRepository regraDividaRepository = new RegraDividaRepositoryImpl();
        DividaRepository dividaRepository = new DividaRepositoryImpl();

        regraDividaService = new RegraDividaService(regraDividaRepository);
        dividaService = new DividaService(dividaRepository, new ClassificacaoService());
        jornadaService = new JornadaService(jornadaRepository, regraDividaService,
                new CobrancaPeriodoService(new ClassificacaoService(), dividaService),
                dividaService);

        Gestor gestor = new Gestor(UUID.randomUUID(), "gestor@teste.pt", "hash", "Gestor de Teste");
        liga = new Liga(UUID.randomUUID(), "Liga de Teste", 10, EstadoLiga.ATIVA, gestor);

        primeira = equipa("Primeira");
        segunda = equipa("Segunda");
        terceira = equipa("Terceira");
        quarta = equipa("Quarta");
        liga.adicionarEquipa(primeira);
        liga.adicionarEquipa(segunda);
        liga.adicionarEquipa(terceira);
        liga.adicionarEquipa(quarta);
    }

    private Equipa equipa(String nome) {
        return new Equipa(UUID.randomUUID(), nome, new Treinador(UUID.randomUUID(), nome + " (treinador)"),
                liga, EstadoEquipa.ATIVA);
    }

    private Jornada abrirEPontuar(int pontosPrimeira, int pontosSegunda, int pontosTerceira, int pontosQuarta) {
        Jornada jornada = jornadaService.abrirJornada(liga);
        jornadaService.inserirResultado(jornada, primeira, pontosPrimeira);
        jornadaService.inserirResultado(jornada, segunda, pontosSegunda);
        jornadaService.inserirResultado(jornada, terceira, pontosTerceira);
        jornadaService.inserirResultado(jornada, quarta, pontosQuarta);
        return jornada;
    }

    /** Escalões de uma equipa: 1º paga 0.00, 2º 0.50, 3º 1.00, 4º 1.50. */
    private void regraDeUmaEquipaPorEscalao() {
        regraDividaService.definir(liga, BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("0.50"), 1, new BigDecimal("2.50"), 1);
    }

    private int posicaoDe(Jornada jornada, Equipa equipa) {
        return resultadoDe(jornada, equipa).getPosicao();
    }

    private ResultadoJornada resultadoDe(Jornada jornada, Equipa equipa) {
        return jornada.getResultadoJ().stream()
                .filter(resultado -> resultado.getEquipa().equals(equipa))
                .findFirst()
                .orElseThrow();
    }

    private BigDecimal totalDe(Equipa equipa) {
        Optional<Divida> divida = dividaService.buscarPorEquipa(equipa);
        return divida.map(dividaService::calcularTotalDivida).orElse(BigDecimal.ZERO);
    }

    @Test
    void jornadaComEmpateFicaAEsperaDeDesempateEmVezDeFechar() {
        Jornada jornada = abrirEPontuar(3, 3, 2, 1);

        jornadaService.fecharJornada(jornada);

        assertEquals(EstadoJornada.DESEMPATE, jornada.getEstadoJ());
    }

    @Test
    void jornadaSemEmpateFechaComoDantes() {
        Jornada jornada = abrirEPontuar(4, 3, 2, 1);

        jornadaService.fecharJornada(jornada);

        assertEquals(EstadoJornada.FECHADA, jornada.getEstadoJ());
    }

    /** O ponto todo do bloqueio: com empate por desfazer não sai dinheiro nenhum. */
    @Test
    void jornadaEmDesempateNaoCobraBlocoNenhum() {
        regraDeUmaEquipaPorEscalao();

        jornadaService.fecharJornada(abrirEPontuar(3, 3, 2, 1));

        assertTrue(dividaService.buscarPorEquipa(primeira).isEmpty());
        assertTrue(dividaService.buscarPorEquipa(segunda).isEmpty());
    }

    @Test
    void resolverDesempateAtribuiPosicoesSequenciaisPelaOrdemDada() {
        Jornada jornada = abrirEPontuar(3, 3, 2, 1);
        jornadaService.fecharJornada(jornada);

        jornadaService.resolverDesempate(jornada, List.of(segunda.getId(), primeira.getId()));

        assertEquals(EstadoJornada.FECHADA, jornada.getEstadoJ());
        assertEquals(1, posicaoDe(jornada, segunda));
        assertEquals(2, posicaoDe(jornada, primeira));
        assertEquals(3, posicaoDe(jornada, terceira));
        assertEquals(4, posicaoDe(jornada, quarta));
    }

    /**
     * A pontuação continua a mandar: a ordem dada só decide entre quem empatou.
     * Dois grupos de empate na mesma jornada resolvem-se numa lista só.
     */
    @Test
    void ordemDadaSoDesempataDentroDeCadaPontuacao() {
        Jornada jornada = abrirEPontuar(5, 5, 2, 2);
        jornadaService.fecharJornada(jornada);

        jornadaService.resolverDesempate(jornada,
                List.of(segunda.getId(), primeira.getId(), quarta.getId(), terceira.getId()));

        assertEquals(1, posicaoDe(jornada, segunda));
        assertEquals(2, posicaoDe(jornada, primeira));
        assertEquals(3, posicaoDe(jornada, quarta));
        assertEquals(4, posicaoDe(jornada, terceira));
    }

    @Test
    void resolverDesempateMarcaSoOsResultadosEmpatados() {
        Jornada jornada = abrirEPontuar(3, 3, 2, 1);
        jornadaService.fecharJornada(jornada);

        jornadaService.resolverDesempate(jornada, List.of(segunda.getId(), primeira.getId()));

        assertTrue(resultadoDe(jornada, primeira).isDesempateManual());
        assertTrue(resultadoDe(jornada, segunda).isDesempateManual());
        assertFalse(resultadoDe(jornada, terceira).isDesempateManual());
        assertFalse(resultadoDe(jornada, quarta).isDesempateManual());
    }

    /**
     * O teste que protege o dinheiro: o bloco só fecha depois do desempate, e
     * com as posições já desfeitas. Empatadas, a primeira e a segunda pagariam
     * as duas o escalão do 1º lugar (0.00) e a liga perdia 0.50.
     */
    @Test
    void oBlocoSoFechaDepoisDoDesempateEComAsPosicoesJaDesfeitas() {
        regraDeUmaEquipaPorEscalao();

        Jornada jornada = abrirEPontuar(3, 3, 2, 1);
        jornadaService.fecharJornada(jornada);
        assertTrue(dividaService.buscarPorEquipa(primeira).isEmpty());

        jornadaService.resolverDesempate(jornada, List.of(segunda.getId(), primeira.getId()));

        assertEquals(BigDecimal.ZERO, totalDe(segunda));
        assertEquals(new BigDecimal("0.50"), totalDe(primeira));
        assertEquals(new BigDecimal("1.00"), totalDe(terceira));
        assertEquals(new BigDecimal("1.50"), totalDe(quarta));
    }

    /**
     * O empate a meio de uma janela de bloco: a jornada 1 fecha e fica
     * pendente, a jornada 2 empata e segura tudo, e quando o desempate é
     * resolvido o bloco fecha com as DUAS jornadas somadas. Cruza o bloqueio
     * novo com a marca incluidaEmBloco, e é dinheiro.
     */
    @Test
    void desempateAMeioDeUmBlocoSeguraAsJornadasAnterioresEDepoisSomaTudo() {
        regraDividaService.definir(liga, BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("0.50"), 1, new BigDecimal("2.50"), 2);

        jornadaService.fecharJornada(abrirEPontuar(4, 3, 2, 1));
        assertTrue(dividaService.buscarPorEquipa(primeira).isEmpty());

        Jornada segundaJornada = abrirEPontuar(5, 5, 2, 1);
        jornadaService.fecharJornada(segundaJornada);
        assertEquals(EstadoJornada.DESEMPATE, segundaJornada.getEstadoJ());
        assertTrue(dividaService.buscarPorEquipa(primeira).isEmpty());

        jornadaService.resolverDesempate(segundaJornada, List.of(segunda.getId(), primeira.getId()));

        // jornada 1: 0.00 / 0.50 / 1.00 / 1.50 (primeira, segunda, terceira, quarta)
        // jornada 2: 0.50 / 0.00 / 1.00 / 1.50 (a segunda passou à frente no desempate)
        assertEquals(new BigDecimal("0.50"), totalDe(primeira));
        assertEquals(new BigDecimal("0.50"), totalDe(segunda));
        assertEquals(new BigDecimal("2.00"), totalDe(terceira));
        assertEquals(new BigDecimal("3.00"), totalDe(quarta));
    }

    @Test
    void naoDeveResolverDesempateDeJornadaQueNaoEstaEmDesempate() {
        Jornada jornada = abrirEPontuar(4, 3, 2, 1);
        jornadaService.fecharJornada(jornada);

        List<UUID> ordem = List.of(primeira.getId(), segunda.getId());
        assertThrows(IllegalStateException.class, () -> jornadaService.resolverDesempate(jornada, ordem));
    }

    @Test
    void naoDeveAceitarOrdemComEquipaQueNaoEstaEmpatada() {
        Jornada jornada = abrirEPontuar(3, 3, 2, 1);
        jornadaService.fecharJornada(jornada);

        List<UUID> comIntruso = List.of(segunda.getId(), primeira.getId(), terceira.getId());
        assertThrows(IllegalArgumentException.class,
                () -> jornadaService.resolverDesempate(jornada, comIntruso));
    }

    @Test
    void naoDeveAceitarOrdemIncompleta() {
        Jornada jornada = abrirEPontuar(3, 3, 2, 1);
        jornadaService.fecharJornada(jornada);

        List<UUID> soUma = List.of(segunda.getId());
        assertThrows(IllegalArgumentException.class, () -> jornadaService.resolverDesempate(jornada, soUma));
    }

    @Test
    void naoDeveAceitarOrdemComEquipaRepetida() {
        Jornada jornada = abrirEPontuar(3, 3, 2, 1);
        jornadaService.fecharJornada(jornada);

        List<UUID> repetida = List.of(segunda.getId(), segunda.getId());
        assertThrows(IllegalArgumentException.class, () -> jornadaService.resolverDesempate(jornada, repetida));
    }

    /**
     * Mexer nas pontuações durante o desempate invalidava-o em silêncio: o
     * empate deixava de ser entre as mesmas equipas e a ordem submetida a
     * seguir já não batia certo com nada.
     */
    @Test
    void naoDeveInserirResultadoEnquantoOEmpateNaoEstiverResolvido() {
        Jornada jornada = abrirEPontuar(3, 3, 2, 1);
        jornadaService.fecharJornada(jornada);

        assertThrows(IllegalStateException.class,
                () -> jornadaService.inserirResultado(jornada, terceira, 7));
    }

    @Test
    void naoDeveFecharUmaJornadaJaAEsperaDeDesempate() {
        Jornada jornada = abrirEPontuar(3, 3, 2, 1);
        jornadaService.fecharJornada(jornada);

        assertThrows(IllegalStateException.class, () -> jornadaService.fecharJornada(jornada));
    }

    /** Enquanto o empate não for resolvido, a jornada conta como por fechar. */
    @Test
    void naoDeveAbrirJornadaNovaComUmDesempatePorResolver() {
        jornadaService.fecharJornada(abrirEPontuar(3, 3, 2, 1));

        assertThrows(IllegalStateException.class, () -> jornadaService.abrirJornada(liga));
    }
}
