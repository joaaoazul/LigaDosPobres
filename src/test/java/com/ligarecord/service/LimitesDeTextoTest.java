package com.ligarecord.service;

import com.ligarecord.domain.Equipa;
import com.ligarecord.domain.Gestor;
import com.ligarecord.domain.Liga;
import com.ligarecord.domain.RegraDivida;
import com.ligarecord.domain.Treinador;
import com.ligarecord.domain.enums.EscalaDivida;
import com.ligarecord.domain.enums.EstadoEquipa;
import com.ligarecord.domain.enums.EstadoLiga;
import com.ligarecord.repository.ConviteRepositoryImpl;
import com.ligarecord.repository.DividaRepositoryImpl;
import com.ligarecord.repository.EquipaRepositoryImpl;
import com.ligarecord.repository.LigaRepositoryImpl;
import com.ligarecord.repository.RegraDividaRepositoryImpl;
import com.ligarecord.repository.TreinadorRepositoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Um texto comprido de mais só era travado pela base de dados, e a violação
 * chegava ao utilizador como "Isto foi alterado por outro pedido ao mesmo
 * tempo. Tenta outra vez." — uma mensagem sobre uma corrida, para quem só
 * escreveu um nome grande, e um convite a tentar outra vez para sempre.
 */
class LimitesDeTextoTest {

    private static final String LONGO = "A".repeat(500);

    private LigaService ligaService;
    private Gestor gestor;

    @BeforeEach
    void setUp() {
        ligaService = new LigaService(new LigaRepositoryImpl(), new EquipaRepositoryImpl(),
                new RegraDividaService(new RegraDividaRepositoryImpl()),
                new DividaService(new DividaRepositoryImpl(), new ClassificacaoService()));
        gestor = new Gestor(UUID.randomUUID(), "gestor@teste.pt", "hash", "Gestor");
    }

    @Test
    void oNomeDaLigaTemLimite() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> ligaService.criarLiga(gestor, LONGO, 10));

        assertTrue(erro.getMessage().contains("120"), erro.getMessage());
    }

    @Test
    void oNomeDaEquipaTemLimite() {
        Liga liga = ligaService.criarLiga(gestor, "Liga", 10);
        Equipa equipa = new Equipa(UUID.randomUUID(), LONGO,
                new Treinador(UUID.randomUUID(), "T"), liga, EstadoEquipa.ATIVA);

        assertThrows(IllegalArgumentException.class, () -> ligaService.adicionarEquipa(liga, equipa));
    }

    @Test
    void oNomeDoTreinadorTemLimite() {
        TreinadorService servico = new TreinadorService(new TreinadorRepositoryImpl());

        assertThrows(IllegalArgumentException.class, () -> servico.novo(LONGO, null));
    }

    @Test
    void oEmailTemLimite() {
        TreinadorService servico = new TreinadorService(new TreinadorRepositoryImpl());

        assertThrows(IllegalArgumentException.class,
                () -> servico.novo("Zé", "a".repeat(200) + "@exemplo.pt"));
    }

    @Test
    void oNomeDaCobrancaTemLimite() {
        RegraDividaService servico = new RegraDividaService(new RegraDividaRepositoryImpl());
        Liga liga = new Liga(UUID.randomUUID(), "Liga", 10, EstadoLiga.ATIVA, gestor);

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> servico.definir(liga, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 1,
                        BigDecimal.ZERO, 1, EscalaDivida.FORMULA, null, true,
                        List.of(new RegraDivida.CobrancaPedida("I".repeat(100), 1,
                                List.of(BigDecimal.ONE)))));

        assertTrue(erro.getMessage().contains("40"), erro.getMessage());
    }

    @Test
    void aNotaDoConviteTemLimite() {
        ConviteService servico = new ConviteService(new ConviteRepositoryImpl());

        assertThrows(IllegalArgumentException.class, () -> servico.criar(gestor, LONGO, 7));
    }

    /** O que cabe continua a caber, e fica sem espaços à volta. */
    @Test
    void oQueCabeContinuaAPassar() {
        assertEquals("Liga do Café", ligaService.criarLiga(gestor, "  Liga do Café  ", 10).getNome());
        assertEquals("A".repeat(120), ligaService.criarLiga(gestor, "A".repeat(120), 10).getNome());
    }
}
