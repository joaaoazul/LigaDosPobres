package com.ligarecord.service;

import com.ligarecord.domain.ClassificacaoGeral;
import com.ligarecord.domain.Equipa;
import com.ligarecord.domain.Gestor;
import com.ligarecord.domain.Jornada;
import com.ligarecord.domain.Liga;
import com.ligarecord.domain.RegraDivida;
import com.ligarecord.domain.Treinador;
import com.ligarecord.domain.enums.EstadoEquipa;
import com.ligarecord.domain.enums.EstadoLiga;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class ClassificacaoServiceTest {

    @Test
    public void equipaDesistenteDeveFicarEmUltimaPosicao(){
        ClassificacaoService classificacaoService = new ClassificacaoService();
        Gestor gestor = new Gestor(UUID.randomUUID(), "gestor@teste.pt", "hash", "Gestor de Teste");
        Liga ligaTeste = new Liga(UUID.randomUUID(), "Teste", 20, EstadoLiga.ATIVA, gestor);
        Treinador joao = new Treinador(UUID.randomUUID(), "João Azul");
        Treinador rica = new Treinador(UUID.randomUUID(), "Ricardo Gomes");
Treinador david = new Treinador(UUID.randomUUID(), "David Livramento");


        Equipa bairroCaixa = new Equipa(UUID.randomUUID(), "Bairro da Caixa FC", joao, ligaTeste, EstadoEquipa.ATIVA);
        Equipa rotundaBarco = new Equipa(UUID.randomUUID(), "Rotunda do Barco FC", rica, ligaTeste, EstadoEquipa.ATIVA);
Equipa real = new Equipa(UUID.randomUUID(), "RealDesistente", david, ligaTeste, EstadoEquipa.DESISTENTE);


        ligaTeste.adicionarEquipa(bairroCaixa);
        ligaTeste.adicionarEquipa(rotundaBarco);
        ligaTeste.adicionarEquipa(real);

        List<ClassificacaoGeral> resultado = classificacaoService.calcularClassificacao(ligaTeste);

        resultado.get(resultado.size() - 1);

        assertEquals(real, resultado.get(resultado.size() - 1).getEquipa());

    }

    /**
     * O exemplo que motivou a regra: 0.00 para os 5 primeiros, 0.50 para os 5
     * seguintes, 1.00 para os seguintes, sem nunca passar de 2.50.
     */
    @Test
    public void calculaOValorPorEscalaoComTecto(){
        ClassificacaoService classificacaoService = new ClassificacaoService();
        Gestor gestor = new Gestor(UUID.randomUUID(), "gestor@teste.pt", "hash", "Gestor de Teste");
        Liga liga = new Liga(UUID.randomUUID(), "Teste", 45, EstadoLiga.ATIVA, gestor);
        RegraDivida regra = new RegraDivida(UUID.randomUUID(), liga, BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("0.50"), 5, new BigDecimal("2.50"), 5);

        assertEquals(new BigDecimal("0.00"), classificacaoService.calcularValor(regra, 1));
        assertEquals(new BigDecimal("0.00"), classificacaoService.calcularValor(regra, 5));
        assertEquals(new BigDecimal("0.50"), classificacaoService.calcularValor(regra, 6));
        assertEquals(new BigDecimal("0.50"), classificacaoService.calcularValor(regra, 10));
        assertEquals(new BigDecimal("1.00"), classificacaoService.calcularValor(regra, 11));
        // 6º escalão (posições 26-30) já daria 2.50, igual ao tecto
        assertEquals(new BigDecimal("2.50"), classificacaoService.calcularValor(regra, 26));
        // 7º escalão passaria dos 2.50 sem o tecto — fica preso nele
        assertEquals(new BigDecimal("2.50"), classificacaoService.calcularValor(regra, 31));
    }


    /**
     * Há ligas em que o treino é um aquecimento: quando as oficiais começam, a
     * tabela recomeça do zero. Nas outras — as que já existiam — os pontos do
     * treino contam, e é esse o valor por omissão.
     */
    @Test
    public void podeIgnorarOsPontosDasJornadasDeTreino() {
        Gestor gestor = new Gestor(UUID.randomUUID(), "gestor@teste.pt", "hash", "Gestor");
        Liga liga = new Liga(UUID.randomUUID(), "Teste", 10, EstadoLiga.ATIVA, gestor);

        Equipa umaEquipa = new Equipa(UUID.randomUUID(), "Uma",
                new Treinador(UUID.randomUUID(), "Treinador"), liga, EstadoEquipa.ATIVA);
        liga.adicionarEquipa(umaEquipa);

        DividaService dividaService = new DividaService(
                new com.ligarecord.repository.DividaRepositoryImpl(), new ClassificacaoService());
        JornadaService jornadaService = new JornadaService(
                new com.ligarecord.repository.JornadaRepositoryImpl(),
                new RegraDividaService(new com.ligarecord.repository.RegraDividaRepositoryImpl()),
                new CobrancaPeriodoService(new ClassificacaoService(), dividaService),
                dividaService);

        // As cinco primeiras são de treino, por desenho da aplicação.
        Jornada treino = jornadaService.abrirJornada(liga);
        jornadaService.inserirResultado(treino, umaEquipa, 7);
        jornadaService.fecharJornada(treino);

        ClassificacaoService servico = new ClassificacaoService();

        assertEquals(7, servico.calcularClassificacao(liga).get(0).getPontosAcumulados());

        liga.setPontosTreinoContam(false);
        assertEquals(0, servico.calcularClassificacao(liga).get(0).getPontosAcumulados());
    }

}
