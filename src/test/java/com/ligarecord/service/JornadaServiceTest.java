package com.ligarecord.service;

import com.ligarecord.domain.Divida;
import com.ligarecord.domain.Equipa;
import com.ligarecord.domain.Gestor;
import com.ligarecord.domain.Jornada;
import com.ligarecord.domain.Liga;
import com.ligarecord.domain.Treinador;
import com.ligarecord.domain.enums.EstadoEquipa;
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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Foca-se na ligação nova entre fechar uma jornada e fechar um bloco de
 * dívida — o resto do comportamento de {@link JornadaService} não tinha
 * testes antes desta feature e fica fora de âmbito aqui.
 */
class JornadaServiceTest {

    private JornadaService jornadaService;
    private DividaService dividaService;
    private RegraDividaService regraDividaService;
    private Gestor gestor;
    private Liga liga;
    private Equipa primeira;
    private Equipa segunda;
    private Equipa terceira;

    @BeforeEach
    void setUp() {
        JornadaRepository jornadaRepository = new JornadaRepositoryImpl();
        RegraDividaRepository regraDividaRepository = new RegraDividaRepositoryImpl();
        DividaRepository dividaRepository = new DividaRepositoryImpl();

        ClassificacaoService classificacaoService = new ClassificacaoService();
        regraDividaService = new RegraDividaService(regraDividaRepository);
        dividaService = new DividaService(dividaRepository, classificacaoService);
        jornadaService = new JornadaService(jornadaRepository, regraDividaService, classificacaoService, dividaService);

        gestor = new Gestor(UUID.randomUUID(), "gestor@teste.pt", "hash", "Gestor de Teste");
        liga = new Liga(UUID.randomUUID(), "Liga de Teste", 10, EstadoLiga.ATIVA, gestor);

        primeira = equipa("Primeira");
        segunda = equipa("Segunda");
        terceira = equipa("Terceira");
        liga.adicionarEquipa(primeira);
        liga.adicionarEquipa(segunda);
        liga.adicionarEquipa(terceira);
    }

    private Equipa equipa(String nome) {
        return new Equipa(UUID.randomUUID(), nome, new Treinador(UUID.randomUUID(), nome + " (treinador)"),
                liga, EstadoEquipa.ATIVA);
    }

    private Jornada abrirEPontuar(int pontosPrimeira, int pontosSegunda, int pontosTerceira) {
        Jornada jornada = jornadaService.abrirJornada(liga);
        jornadaService.inserirResultado(jornada, primeira, pontosPrimeira);
        jornadaService.inserirResultado(jornada, segunda, pontosSegunda);
        jornadaService.inserirResultado(jornada, terceira, pontosTerceira);
        return jornada;
    }

    @Test
    void semRegraDeDividaFecharJornadaNaoCriaBlocoNenhum() {
        Jornada jornada = abrirEPontuar(3, 2, 1);
        jornadaService.fecharJornada(jornada);

        assertTrue(dividaService.buscarPorEquipa(primeira).isEmpty());
    }

    /** Com jornadasPorBloco=1, cada jornada fechada fecha logo um bloco. */
    @Test
    void fechaBlocoAutomaticamenteQuandoAJornadaFechaOBloco() {
        regraDividaService.definir(liga, BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("0.50"), 1, new BigDecimal("2.50"), 1);

        Jornada jornada = abrirEPontuar(3, 2, 1);
        jornadaService.fecharJornada(jornada);

        // 1ª posição: 0.00; 2ª: 0.50; 3ª: 1.00 (equipasPorEscalao = 1)
        assertEquals(new BigDecimal("0.00"), totalDe(primeira));
        assertEquals(new BigDecimal("0.50"), totalDe(segunda));
        assertEquals(new BigDecimal("1.00"), totalDe(terceira));
    }

    /** Com jornadasPorBloco=2, só a 2ª jornada fechada fecha o bloco. */
    @Test
    void soFechaOBlocoNaJornadaCertaDaPeriodicidade() {
        regraDividaService.definir(liga, BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("0.50"), 1, new BigDecimal("2.50"), 2);

        Jornada primeiraJornada = abrirEPontuar(3, 2, 1);
        jornadaService.fecharJornada(primeiraJornada);
        assertTrue(dividaService.buscarPorEquipa(primeira).isEmpty());

        Jornada segundaJornada = abrirEPontuar(1, 2, 3);
        jornadaService.fecharJornada(segundaJornada);
        // agora é a terceira equipa que vai em 1º (3 pontos na 2ª jornada, mas só 1 na 1ª: 1+3=4)
        assertTrue(dividaService.buscarPorEquipa(primeira).isPresent());
    }

    /** Uma equipa que já desistiu não volta a ser cobrada no fecho automático. */
    @Test
    void naoCobraEquipaDesistenteNoFechoAutomatico() {
        regraDividaService.definir(liga, BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("0.50"), 1, new BigDecimal("2.50"), 1);
        terceira.setEstado(EstadoEquipa.DESISTENTE);

        // uma equipa desistente não tem resultados novos — só as activas jogam
        Jornada jornada = jornadaService.abrirJornada(liga);
        jornadaService.inserirResultado(jornada, primeira, 3);
        jornadaService.inserirResultado(jornada, segunda, 2);
        jornadaService.fecharJornada(jornada);

        assertTrue(dividaService.buscarPorEquipa(terceira).isEmpty());
        assertTrue(dividaService.buscarPorEquipa(primeira).isPresent());
    }

    private BigDecimal totalDe(Equipa equipa) {
        Optional<Divida> divida = dividaService.buscarPorEquipa(equipa);
        return divida.map(dividaService::calcularTotalDivida).orElse(BigDecimal.ZERO);
    }
}
