package com.ligarecord.service;

import com.ligarecord.domain.Gestor;
import com.ligarecord.domain.Liga;
import com.ligarecord.domain.RegraDivida;
import com.ligarecord.domain.enums.EstadoLiga;
import com.ligarecord.repository.RegraDividaRepository;
import com.ligarecord.repository.RegraDividaRepositoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RegraDividaServiceTest {

    private RegraDividaRepository regraDividaRepository;
    private RegraDividaService regraDividaService;
    private Liga liga;

    @BeforeEach
    void setUp() {
        regraDividaRepository = new RegraDividaRepositoryImpl();
        regraDividaService = new RegraDividaService(regraDividaRepository);

        Gestor gestor = new Gestor(UUID.randomUUID(), "gestor@teste.pt", "hash", "Gestor");
        liga = new Liga(UUID.randomUUID(), "Liga de Teste", 10, EstadoLiga.ATIVA, gestor);
    }

    @Test
    void deveDefinirUmaRegraNova() {
        RegraDivida regra = regraDividaService.definir(liga,
                new BigDecimal("5.00"), BigDecimal.ZERO, new BigDecimal("0.50"), 5,
                new BigDecimal("2.50"), 5);

        assertEquals(liga, regra.getLiga());
        assertEquals(new BigDecimal("5.00"), regra.getValorInscricao());
        assertEquals(5, regra.getEquipasPorEscalao());
        assertEquals(5, regra.getJornadasPorBloco());
    }

    /** Ajustar valores a meio da época não pode duplicar a regra da liga. */
    @Test
    void deveSubstituirARegraExistenteEmVezDeCriarOutra() {
        regraDividaService.definir(liga, BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("0.50"), 5, new BigDecimal("2.50"), 5);
        regraDividaService.definir(liga, BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("1.00"), 5, new BigDecimal("5.00"), 3);

        RegraDivida regra = regraDividaService.buscarPorLiga(liga).orElseThrow();
        assertEquals(new BigDecimal("1.00"), regra.getIncremento());
        assertEquals(new BigDecimal("5.00"), regra.getValorMaximo());
        assertEquals(3, regra.getJornadasPorBloco());
    }

    @Test
    void naoDeveAceitarValorNegativo() {
        assertThrows(IllegalArgumentException.class, () -> regraDividaService.definir(liga,
                new BigDecimal("-1"), BigDecimal.ZERO, new BigDecimal("0.50"), 5,
                new BigDecimal("2.50"), 5));
    }

    @Test
    void naoDeveAceitarEquipasPorEscalaoMenorQueUm() {
        assertThrows(IllegalArgumentException.class, () -> regraDividaService.definir(liga,
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("0.50"), 0,
                new BigDecimal("2.50"), 5));
    }

    @Test
    void naoDeveAceitarJornadasPorBlocoMenorQueUm() {
        assertThrows(IllegalArgumentException.class, () -> regraDividaService.definir(liga,
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("0.50"), 5,
                new BigDecimal("2.50"), 0));
    }

    @Test
    void naoDeveAceitarValorMaximoMenorQueOInicial() {
        assertThrows(IllegalArgumentException.class, () -> regraDividaService.definir(liga,
                BigDecimal.ZERO, new BigDecimal("3.00"), new BigDecimal("0.50"), 5,
                new BigDecimal("2.50"), 5));
    }
}
