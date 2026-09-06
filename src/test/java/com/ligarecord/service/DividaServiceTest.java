package com.ligarecord.service;

import com.ligarecord.domain.BlocoDivida;
import com.ligarecord.domain.Divida;
import com.ligarecord.domain.Equipa;
import com.ligarecord.domain.Gestor;
import com.ligarecord.domain.Liga;
import com.ligarecord.domain.Treinador;
import com.ligarecord.domain.enums.EstadoDivida;
import com.ligarecord.domain.enums.EstadoEquipa;
import com.ligarecord.domain.enums.EstadoLiga;
import com.ligarecord.repository.DividaRepository;
import com.ligarecord.repository.DividaRepositoryImpl;
import com.ligarecord.web.RecursoNaoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DividaServiceTest {

    private DividaRepository dividaRepository;
    private DividaService dividaService;
    private Gestor dono;
    private Liga liga;
    private Treinador treinador;
    private Equipa equipa;

    @BeforeEach
    void setUp() {
        dividaRepository = new DividaRepositoryImpl();
        dividaService = new DividaService(dividaRepository, new ClassificacaoService());

        dono = new Gestor(UUID.randomUUID(), "gestor@teste.pt", "hash", "Gestor");
        liga = new Liga(UUID.randomUUID(), "Liga de Teste", 10, EstadoLiga.ATIVA, dono);
        treinador = new Treinador(UUID.randomUUID(), "João Azul");
        equipa = new Equipa(UUID.randomUUID(), "Equipa de Teste", treinador, liga, EstadoEquipa.ATIVA);
    }

    @Test
    void primeiroBlocoCriaADividaComNumeroUm() {
        BlocoDivida bloco = dividaService.registarBloco(equipa, new BigDecimal("2.50"));

        assertEquals(1, bloco.getNumeroBloco());
        assertEquals(EstadoDivida.PENDENTE, bloco.getEstado());
        assertTrue(dividaRepository.buscarPorEquipa(equipa).isPresent());
    }

    @Test
    void blocosSeguintesIncrementamONumero() {
        dividaService.registarBloco(equipa, new BigDecimal("1.00"));
        dividaService.registarBloco(equipa, new BigDecimal("1.50"));
        BlocoDivida terceiro = dividaService.registarBloco(equipa, new BigDecimal("2.00"));

        assertEquals(3, terceiro.getNumeroBloco());
        assertEquals(3, dividaRepository.buscarPorEquipa(equipa).orElseThrow().getBlocos().size());
    }

    /** Nada para cobrar não devia exigir que o gestor marque nada como pago. */
    @Test
    void blocoDeValorZeroFicaLogoResolvido() {
        BlocoDivida bloco = dividaService.registarBloco(equipa, BigDecimal.ZERO);

        assertTrue(bloco.estaResolvido());
    }

    /**
     * Apanhado a testar no browser: a dívida nasce PENDENTE no construtor, por
     * isso uma equipa cujo primeiro (e único) bloco é de 0.00€ ficava marcada
     * como pendente a dever zero — e o gestor não tinha sequer o que marcar
     * como pago.
     */
    @Test
    void dividaCujoPrimeiroBlocoEDeValorZeroNaoFicaPendente() {
        dividaService.registarBloco(equipa, BigDecimal.ZERO);

        Divida divida = dividaRepository.buscarPorEquipa(equipa).orElseThrow();
        assertEquals(EstadoDivida.RESOLVIDA, divida.getEstado());
    }

    /** Um bloco de 0.00€ novo não devia reabrir uma dívida já paga. */
    @Test
    void blocoDeValorZeroNaoReabreADividaJaResolvida() {
        BlocoDivida unico = dividaService.registarBloco(equipa, new BigDecimal("1.00"));
        dividaService.resolverBloco(equipa, unico.getId());

        dividaService.registarBloco(equipa, BigDecimal.ZERO);

        Divida divida = dividaRepository.buscarPorEquipa(equipa).orElseThrow();
        assertEquals(EstadoDivida.RESOLVIDA, divida.getEstado());
    }

    @Test
    void naoDeveRegistarBlocoComValorNegativo() {
        assertThrows(
                IllegalArgumentException.class,
                () -> dividaService.registarBloco(equipa, new BigDecimal("-1.00")));
    }

    @Test
    void totalDaDividaSoContaBlocosPendentes() {
        BlocoDivida primeiro = dividaService.registarBloco(equipa, new BigDecimal("1.00"));
        dividaService.registarBloco(equipa, new BigDecimal("2.00"));
        dividaService.resolverBloco(equipa, primeiro.getId());

        Divida divida = dividaRepository.buscarPorEquipa(equipa).orElseThrow();

        assertEquals(new BigDecimal("2.00"), dividaService.calcularTotalDivida(divida));
    }

    @Test
    void resolverBlocoNaoAfectaOsOutros() {
        BlocoDivida primeiro = dividaService.registarBloco(equipa, new BigDecimal("1.00"));
        BlocoDivida segundo = dividaService.registarBloco(equipa, new BigDecimal("2.00"));

        dividaService.resolverBloco(equipa, primeiro.getId());

        Divida divida = dividaRepository.buscarPorEquipa(equipa).orElseThrow();
        assertEquals(EstadoDivida.PENDENTE, divida.getEstado());
        assertTrue(segundo.getEstado() == EstadoDivida.PENDENTE);
    }

    /** Só quando não sobra nenhum bloco por pagar é que a dívida fica resolvida. */
    @Test
    void dividaFicaResolvidaQuandoTodosOsBlocosEstaoPagos() {
        BlocoDivida unico = dividaService.registarBloco(equipa, new BigDecimal("1.00"));
        dividaService.resolverBloco(equipa, unico.getId());

        Divida divida = dividaRepository.buscarPorEquipa(equipa).orElseThrow();
        assertEquals(EstadoDivida.RESOLVIDA, divida.getEstado());
    }

    @Test
    void naoDeveResolverOMesmoBlocoDuasVezes() {
        BlocoDivida bloco = dividaService.registarBloco(equipa, new BigDecimal("1.00"));
        dividaService.resolverBloco(equipa, bloco.getId());

        assertThrows(IllegalStateException.class, () -> dividaService.resolverBloco(equipa, bloco.getId()));
    }

    @Test
    void naoDeveResolverBlocoInexistente() {
        dividaService.registarBloco(equipa, new BigDecimal("1.00"));

        assertThrows(
                RecursoNaoEncontradoException.class,
                () -> dividaService.resolverBloco(equipa, UUID.randomUUID()));
    }

    @Test
    void naoDeveResolverDividaDeEquipaSemDivida() {
        Equipa outra = new Equipa(UUID.randomUUID(), "Sem dívida", treinador, liga, EstadoEquipa.ATIVA);

        assertThrows(RecursoNaoEncontradoException.class, () -> dividaService.resolverDivida(outra));
    }

    /** Um pagamento único cobre tudo o que a equipa ainda deve, de uma vez. */
    @Test
    void resolverDividaPagaTodosOsBlocosPendentes() {
        dividaService.registarBloco(equipa, new BigDecimal("1.00"));
        dividaService.registarBloco(equipa, new BigDecimal("2.00"));

        Divida divida = dividaService.resolverDivida(equipa);

        assertEquals(EstadoDivida.RESOLVIDA, divida.getEstado());
        assertTrue(divida.getBlocos().stream().allMatch(BlocoDivida::estaResolvido));
        assertEquals(BigDecimal.ZERO, dividaService.calcularTotalDivida(divida));
    }

    /** O caso central: o treinador vê as dívidas de todas as equipas que treina, em qualquer liga. */
    @Test
    void listaDividasDeTodasAsEquipasDoTreinador() {
        Gestor contaTreinador = new Gestor(UUID.randomUUID(), "treinador@teste.pt", "hash", "Treinador");
        treinador.setConta(contaTreinador);

        Liga outraLiga = new Liga(UUID.randomUUID(), "Outra Liga", 10, EstadoLiga.ATIVA, dono);
        Equipa segundaEquipa = new Equipa(UUID.randomUUID(), "Segunda Equipa", treinador, outraLiga, EstadoEquipa.ATIVA);

        dividaService.registarBloco(equipa, new BigDecimal("1.00"));
        dividaService.registarBloco(segundaEquipa, new BigDecimal("2.00"));

        List<Divida> dividas = dividaService.listarPorTreinador(contaTreinador);

        assertEquals(2, dividas.size());
        assertEquals(new BigDecimal("3.00"), dividaService.calcularTotalTreinador(contaTreinador));
    }

    @Test
    void naoListaDividasDeEquipasDeOutroTreinador() {
        Gestor contaTreinador = new Gestor(UUID.randomUUID(), "treinador@teste.pt", "hash", "Treinador");
        treinador.setConta(contaTreinador);
        dividaService.registarBloco(equipa, new BigDecimal("1.00"));

        Gestor outraConta = new Gestor(UUID.randomUUID(), "outro@teste.pt", "hash", "Outro");

        assertTrue(dividaService.listarPorTreinador(outraConta).isEmpty());
    }
}
