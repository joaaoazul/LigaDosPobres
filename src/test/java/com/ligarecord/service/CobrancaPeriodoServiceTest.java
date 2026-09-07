package com.ligarecord.service;

import com.ligarecord.domain.Equipa;
import com.ligarecord.domain.Gestor;
import com.ligarecord.domain.Jornada;
import com.ligarecord.domain.Liga;
import com.ligarecord.domain.RegraDivida;
import com.ligarecord.domain.Treinador;
import com.ligarecord.domain.enums.EscalaDivida;
import com.ligarecord.domain.enums.EstadoEquipa;
import com.ligarecord.domain.enums.EstadoLiga;
import com.ligarecord.repository.DividaRepositoryImpl;
import com.ligarecord.repository.JornadaRepositoryImpl;
import com.ligarecord.repository.RegraDividaRepositoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * O "Inverno" e o "Verão": cobrados numa jornada oficial combinada, pela
 * posição na classificação geral.
 */
class CobrancaPeriodoServiceTest {

    /** 1º não paga, e sobe 0,50€ por lugar — a tabela da liga que motivou isto. */
    private static final String TABELA_INVERNO = "1-0€\n2-0,50€\n3-1€\n4-1,50€\n5-2€";

    private JornadaService jornadaService;
    private DividaService dividaService;
    private RegraDividaService regraDividaService;
    private CobrancaPeriodoService cobrancaService;
    private Liga liga;
    private Equipa primeira;
    private Equipa segunda;
    private Equipa terceira;

    @BeforeEach
    void setUp() {
        ClassificacaoService classificacaoService = new ClassificacaoService();
        dividaService = new DividaService(new DividaRepositoryImpl(), classificacaoService);
        regraDividaService = new RegraDividaService(new RegraDividaRepositoryImpl());
        cobrancaService = new CobrancaPeriodoService(classificacaoService, dividaService);
        jornadaService = new JornadaService(new JornadaRepositoryImpl(), regraDividaService,
                cobrancaService, dividaService);

        Gestor gestor = new Gestor(UUID.randomUUID(), "gestor@teste.pt", "hash", "Gestor");
        liga = new Liga(UUID.randomUUID(), "Liga do Café", 10, EstadoLiga.ATIVA, gestor);
        // O treino é um aquecimento nesta liga: não paga e não conta pontos.
        liga.setPontosTreinoContam(false);

        primeira = equipa("Alfa");
        segunda = equipa("Bravo");
        terceira = equipa("Charlie");
        liga.adicionarEquipa(primeira);
        liga.adicionarEquipa(segunda);
        liga.adicionarEquipa(terceira);
    }

    private Equipa equipa(String nome) {
        return new Equipa(UUID.randomUUID(), nome, new Treinador(UUID.randomUUID(), nome), liga,
                EstadoEquipa.ATIVA);
    }

    /** Cobrança na 2ª oficial, para os testes não terem de jogar 12 jornadas. */
    private RegraDivida regraComInverno(int jornadaOficial) {
        return regraDividaService.definir(liga, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 1,
                BigDecimal.ZERO, 1, EscalaDivida.TABELA, EscalaColada.ler("1-0€\n2-0€\n3-0€"), false,
                List.of(new RegraDivida.CobrancaPedida("Inverno", jornadaOficial,
                        EscalaColada.ler(TABELA_INVERNO))));
    }

    private Jornada jogar(int pontosPrimeira, int pontosSegunda, int pontosTerceira) {
        Jornada jornada = jornadaService.abrirJornada(liga);
        jornadaService.inserirResultado(jornada, primeira, pontosPrimeira);
        jornadaService.inserirResultado(jornada, segunda, pontosSegunda);
        jornadaService.inserirResultado(jornada, terceira, pontosTerceira);
        return jornadaService.fecharJornada(jornada);
    }

    /**
     * Deixa Alfa e Bravo empatados na geral com 6 pontos e Charlie com 3, sem
     * nunca empatar dentro de uma jornada — uma jornada empatada nem sequer
     * fecha (fica em DESEMPATE), e então não havia cobrança nenhuma a testar.
     */
    private void empatarAlfaEBravoNaGeral() {
        jogar(5, 3, 1);   // oficial 1: A5 B3 C1
        jogar(1, 3, 2);   // oficial 2: A6 B6 C3
    }

    /** As cinco de treino, para chegar às oficiais. */
    private void passarOTreino() {
        for (int i = 0; i < 5; i++) {
            jogar(3, 2, 1);
        }
    }

    private BigDecimal totalDe(Equipa equipa) {
        return dividaService.buscarPorEquipa(equipa)
                .map(dividaService::calcularTotalDivida)
                .orElse(BigDecimal.ZERO);
    }

    @Test
    void cobraNaJornadaOficialCombinadaEPelaClassificacaoGeral() {
        RegraDivida regra = regraComInverno(2);
        passarOTreino();

        jogar(3, 2, 1);   // oficial 1 — ainda não
        assertFalse(regra.getCobrancas().get(0).estaCobrada());

        jogar(3, 2, 1);   // oficial 2 — é aqui
        assertTrue(regra.getCobrancas().get(0).estaCobrada());

        // Geral: Alfa 6, Bravo 4, Charlie 2 (o treino não conta)
        assertEquals(0, totalDe(primeira).compareTo(new BigDecimal("0")));
        assertEquals(0, totalDe(segunda).compareTo(new BigDecimal("0.50")));
        assertEquals(0, totalDe(terceira).compareTo(new BigDecimal("1.00")));
    }

    /** O bloco leva o nome, para na dívida se ler "Inverno" e não "Bloco 3". */
    @Test
    void oBlocoFicaComONomeDaCobranca() {
        regraComInverno(1);
        passarOTreino();
        jogar(3, 2, 1);

        assertTrue(dividaService.buscarPorEquipa(segunda).orElseThrow().getBlocos().stream()
                .anyMatch(bloco -> "Inverno".equals(bloco.getNome())));
    }

    @Test
    void naoCobraDuasVezes() {
        RegraDivida regra = regraComInverno(1);
        passarOTreino();
        jogar(3, 2, 1);
        BigDecimal depoisDaPrimeira = totalDe(segunda);

        // a cobrança já está feita; fechar outra jornada não lhe volta a mexer
        jogar(3, 2, 1);

        assertEquals(0, totalDe(segunda).compareTo(depoisDaPrimeira));
        assertEquals(1, regra.getCobrancas().get(0).getTabela().isEmpty() ? 0 : 1);
    }

    /** As de treino nunca disparam uma cobrança, mesmo com o mesmo número. */
    @Test
    void asJornadasDeTreinoNaoDisparamACobranca() {
        RegraDivida regra = regraComInverno(1);

        jogar(3, 2, 1);   // treino 1

        assertFalse(regra.getCobrancas().get(0).estaCobrada());
    }

    /**
     * Um empate que muda o valor trava a cobrança: cobrar com ele por desfazer
     * era cobrar a mais a umas e a menos a outras.
     */
    @Test
    void umEmpateQueMudaOValorTravaACobranca() {
        RegraDivida regra = regraComInverno(2);
        passarOTreino();

        empatarAlfaEBravoNaGeral();

        assertFalse(regra.getCobrancas().get(0).estaCobrada());
        assertEquals(0, totalDe(segunda).compareTo(BigDecimal.ZERO));
    }

    /** Um empate entre posições que pagam o mesmo não trava nada. */
    @Test
    void umEmpateQueNaoMudaOValorNaoTravaNada() {
        RegraDivida regra = regraDividaService.definir(liga, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, 1, BigDecimal.ZERO, 1, EscalaDivida.TABELA,
                EscalaColada.ler("1-0€\n2-0€\n3-0€"), false,
                List.of(new RegraDivida.CobrancaPedida("Inverno", 2, EscalaColada.ler("1-2€\n2-2€\n3-2€"))));
        passarOTreino();

        empatarAlfaEBravoNaGeral();

        assertTrue(regra.getCobrancas().get(0).estaCobrada());
        assertEquals(0, totalDe(primeira).compareTo(new BigDecimal("2")));
    }

    @Test
    void desempatarCobraPelaOrdemQueOGestorDeu() {
        RegraDivida regra = regraComInverno(2);
        passarOTreino();
        empatarAlfaEBravoNaGeral();

        // O gestor põe o Bravo à frente do Alfa, ao contrário da ordem alfabética
        cobrancaService.resolverDesempateECobrar(regra.getCobrancas().get(0), liga,
                List.of(segunda.getId(), primeira.getId()));

        assertTrue(regra.getCobrancas().get(0).estaCobrada());
        assertEquals(0, totalDe(segunda).compareTo(new BigDecimal("0")));
        assertEquals(0, totalDe(primeira).compareTo(new BigDecimal("0.50")));
    }

    @Test
    void aOrdemTemDeSerExactamenteAsEquipasEmpatadas() {
        RegraDivida regra = regraComInverno(2);
        passarOTreino();
        empatarAlfaEBravoNaGeral();

        assertThrows(IllegalArgumentException.class,
                () -> cobrancaService.resolverDesempateECobrar(regra.getCobrancas().get(0), liga,
                        List.of(segunda.getId(), terceira.getId())));
    }

    @Test
    void naoSeDesempataOQueNaoEstaEmpatado() {
        RegraDivida regra = regraComInverno(1);
        passarOTreino();
        jogar(3, 2, 1);

        assertThrows(IllegalStateException.class,
                () -> cobrancaService.resolverDesempateECobrar(regra.getCobrancas().get(0), liga,
                        List.of(primeira.getId(), segunda.getId())));
    }

    /** Uma cobrança já feita não se apaga nem muda de jornada: o dinheiro está lançado. */
    @Test
    void umaCobrancaFeitaNaoPodeSerRemovidaNemMudada() {
        RegraDivida regra = regraComInverno(1);
        passarOTreino();
        jogar(3, 2, 1);

        assertThrows(IllegalStateException.class, () -> regra.acertarCobrancas(List.of()));
        assertThrows(IllegalStateException.class, () -> regra.acertarCobrancas(
                List.of(new RegraDivida.CobrancaPedida("Inverno", 9, EscalaColada.ler(TABELA_INVERNO)))));
    }

    /** Guardar a regra outra vez não faz uma cobrança já feita renascer por cobrar. */
    @Test
    void guardarARegraOutraVezNaoRepeteACobranca() {
        RegraDivida regra = regraComInverno(1);
        passarOTreino();
        jogar(3, 2, 1);
        BigDecimal depois = totalDe(terceira);

        regraComInverno(1);

        assertTrue(regra.getCobrancas().get(0).estaCobrada());
        assertEquals(0, totalDe(terceira).compareTo(depois));
    }
}
