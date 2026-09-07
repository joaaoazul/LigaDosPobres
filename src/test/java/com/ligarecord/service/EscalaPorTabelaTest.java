package com.ligarecord.service;

import com.ligarecord.domain.Gestor;
import com.ligarecord.domain.Liga;
import com.ligarecord.domain.RegraDivida;
import com.ligarecord.domain.enums.EscalaDivida;
import com.ligarecord.domain.enums.EstadoLiga;
import com.ligarecord.repository.RegraDividaRepositoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * A tabela da liga que motivou isto: sobe 0,20€ por lugar até ao 7º e 0,10€ daí
 * em diante. Não há fórmula que diga aquilo, e é esse o ponto.
 */
class EscalaPorTabelaTest {

    private static final String TABELA_SEMANAL = """
            1-0€
            2-0,10€
            3-0,30€
            4-0,50€
            5-0,70€
            6-0,90€
            7-1,10€
            8-1,20€
            9-1,30€
            10-1,40€
            11-1,50€
            12-1,60€
            13-1,70€
            14-1,80€
            15-1,90€
            16-2,00€
            17-2,10€
            18-2,20€
            19-2,30€
            20-2,40€
            21-2,50€
            """;

    private RegraDividaService servico;
    private ClassificacaoService classificacaoService;
    private Liga liga;

    @BeforeEach
    void setUp() {
        servico = new RegraDividaService(new RegraDividaRepositoryImpl());
        classificacaoService = new ClassificacaoService();

        Gestor gestor = new Gestor(UUID.randomUUID(), "gestor@teste.pt", "hash", "Gestor");
        liga = new Liga(UUID.randomUUID(), "Liga do Café", 21, EstadoLiga.ATIVA, gestor);
    }

    private RegraDivida comTabela(String texto) {
        return servico.definir(liga, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 1,
                BigDecimal.ZERO, 1, EscalaDivida.TABELA, EscalaColada.ler(texto), false);
    }

    @Test
    void cadaPosicaoPagaOQueATabelaDiz() {
        RegraDivida regra = comTabela(TABELA_SEMANAL);

        assertEquals(0, classificacaoService.calcularValor(regra, 1).compareTo(new BigDecimal("0")));
        assertEquals(0, classificacaoService.calcularValor(regra, 2).compareTo(new BigDecimal("0.10")));
        // o degrau de 0,20 que nenhuma fórmula da aplicação conseguia dar
        assertEquals(0, classificacaoService.calcularValor(regra, 3).compareTo(new BigDecimal("0.30")));
        assertEquals(0, classificacaoService.calcularValor(regra, 7).compareTo(new BigDecimal("1.10")));
        // e a partir daqui volta a subir de 0,10
        assertEquals(0, classificacaoService.calcularValor(regra, 8).compareTo(new BigDecimal("1.20")));
        assertEquals(0, classificacaoService.calcularValor(regra, 21).compareTo(new BigDecimal("2.50")));
    }

    /** A soma das 21 posições é o que a liga inteira paga por jornada: 29,50€. */
    @Test
    void aTabelaSomaOQueOPapelDiz() {
        RegraDivida regra = comTabela(TABELA_SEMANAL);

        BigDecimal total = BigDecimal.ZERO;
        for (int posicao = 1; posicao <= 21; posicao++) {
            total = total.add(classificacaoService.calcularValor(regra, posicao));
        }

        assertEquals(0, total.compareTo(new BigDecimal("29.50")));
    }

    /**
     * Uma equipa a mais do que as linhas escritas paga a última. Não cobrar
     * nada premiava exactamente quem ficou em último.
     */
    @Test
    void abaixoDoFimDaTabelaRepeteSeAUltimaLinha() {
        RegraDivida regra = comTabela(TABELA_SEMANAL);

        assertEquals(0, classificacaoService.calcularValor(regra, 22).compareTo(new BigDecimal("2.50")));
        assertEquals(0, classificacaoService.calcularValor(regra, 40).compareTo(new BigDecimal("2.50")));
    }

    /** A tabela substitui-se inteira; não fica meia tabela antiga por baixo. */
    @Test
    void guardarOutraTabelaSubstituiAAnterior() {
        comTabela(TABELA_SEMANAL);
        RegraDivida regra = comTabela("1-0€\n2-5€");

        assertEquals(2, regra.getTabela().size());
        assertEquals(0, classificacaoService.calcularValor(regra, 2).compareTo(new BigDecimal("5")));
    }

    /**
     * Guardar a mesma regra outra vez tem de funcionar. Apagar as linhas e
     * voltar a criá-las dava um conflito de unicidade contra a base de dados
     * real — o Hibernate insere antes de apagar no mesmo commit.
     */
    @Test
    void guardarDuasVezesSeguidasNaoDuplicaPosicoes() {
        comTabela(TABELA_SEMANAL);
        RegraDivida regra = comTabela(TABELA_SEMANAL);

        assertEquals(21, regra.getTabela().size());
        assertEquals(21, regra.getTabela().stream().map(l -> l.getPosicao()).distinct().count());
    }

    /** E uma tabela mais curta a seguir a uma longa não deixa a cauda para trás. */
    @Test
    void umaTabelaMaisCurtaDeitaForaOQueSobra() {
        comTabela(TABELA_SEMANAL);
        RegraDivida regra = comTabela("1-0€\n2-1€\n3-2€");

        assertEquals(3, regra.getTabela().size());
        assertEquals(0, classificacaoService.calcularValor(regra, 21).compareTo(new BigDecimal("2")));
    }

    @Test
    void umaRegraPorTabelaPrecisaDaTabela() {
        assertThrows(IllegalArgumentException.class,
                () -> servico.definir(liga, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 1,
                        BigDecimal.ZERO, 1, EscalaDivida.TABELA, List.of(), true));
    }

    /** A fórmula continua a ser o que sempre foi — nada disto lhe mexeu. */
    @Test
    void aFormulaContinuaIgual() {
        RegraDivida regra = servico.definir(liga, BigDecimal.ZERO, new BigDecimal("0.50"),
                new BigDecimal("0.50"), 5, new BigDecimal("2.50"), 5);

        assertEquals(EscalaDivida.FORMULA, regra.getEscala());
        assertEquals(0, classificacaoService.calcularValor(regra, 1).compareTo(new BigDecimal("0.50")));
        assertEquals(0, classificacaoService.calcularValor(regra, 6).compareTo(new BigDecimal("1.00")));
        // travado no máximo
        assertEquals(0, classificacaoService.calcularValor(regra, 60).compareTo(new BigDecimal("2.50")));
        // e as jornadas de treino continuam a ser cobradas por omissão
        assertEquals(true, regra.isCobraTreino());
    }
}
