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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


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

    /**
     * Sem desempate resolvido, o nome decide — é o comportamento de sempre.
     * Depois de resolvido, passa a decidir ele, mesmo continuando ambas com
     * os mesmos pontos.
     */
    @Test
    public void aplicaODesempateManualEntreEquipasEmpatadas() {
        ClassificacaoService classificacaoService = new ClassificacaoService();
        Gestor gestor = new Gestor(UUID.randomUUID(), "gestor@teste.pt", "hash", "Gestor");
        Liga liga = new Liga(UUID.randomUUID(), "Teste", 10, EstadoLiga.ATIVA, gestor);

        Equipa alfa = new Equipa(UUID.randomUUID(), "Alfa", new Treinador(UUID.randomUUID(), "T1"), liga, EstadoEquipa.ATIVA);
        Equipa beta = new Equipa(UUID.randomUUID(), "Beta", new Treinador(UUID.randomUUID(), "T2"), liga, EstadoEquipa.ATIVA);
        liga.adicionarEquipa(alfa);
        liga.adicionarEquipa(beta);

        assertEquals(alfa, classificacaoService.calcularClassificacao(liga).get(0).getEquipa());

        classificacaoService.aplicarDesempate(liga, List.of(beta.getId(), alfa.getId()));

        assertEquals(beta, classificacaoService.calcularClassificacao(liga).get(0).getEquipa());
    }

    /** Uma equipa desistente não compete pela tabela — não entra em grupo nenhum. */
    @Test
    public void gruposEmpatadosIgnoraEquipasDesistentes() {
        ClassificacaoService classificacaoService = new ClassificacaoService();
        Gestor gestor = new Gestor(UUID.randomUUID(), "gestor@teste.pt", "hash", "Gestor");
        Liga liga = new Liga(UUID.randomUUID(), "Teste", 10, EstadoLiga.ATIVA, gestor);

        Equipa ativa = new Equipa(UUID.randomUUID(), "Ativa", new Treinador(UUID.randomUUID(), "T1"), liga, EstadoEquipa.ATIVA);
        Equipa desistente = new Equipa(UUID.randomUUID(), "Desistente", new Treinador(UUID.randomUUID(), "T2"), liga, EstadoEquipa.DESISTENTE);
        liga.adicionarEquipa(ativa);
        liga.adicionarEquipa(desistente);

        assertTrue(classificacaoService.gruposEmpatados(liga).isEmpty());
    }

    @Test
    public void recusaResolverDesempateSemEquipasEmpatadas() {
        ClassificacaoService classificacaoService = new ClassificacaoService();
        Gestor gestor = new Gestor(UUID.randomUUID(), "gestor@teste.pt", "hash", "Gestor");
        Liga liga = new Liga(UUID.randomUUID(), "Teste", 10, EstadoLiga.ATIVA, gestor);
        Equipa unica = new Equipa(UUID.randomUUID(), "Única", new Treinador(UUID.randomUUID(), "T1"), liga, EstadoEquipa.ATIVA);
        liga.adicionarEquipa(unica);

        assertThrows(IllegalStateException.class,
                () -> classificacaoService.aplicarDesempate(liga, List.of(unica.getId())));
    }

    @Test
    public void recusaOrdemQueNaoBateComAsEquipasEmpatadas() {
        ClassificacaoService classificacaoService = new ClassificacaoService();
        Gestor gestor = new Gestor(UUID.randomUUID(), "gestor@teste.pt", "hash", "Gestor");
        Liga liga = new Liga(UUID.randomUUID(), "Teste", 10, EstadoLiga.ATIVA, gestor);
        Equipa alfa = new Equipa(UUID.randomUUID(), "Alfa", new Treinador(UUID.randomUUID(), "T1"), liga, EstadoEquipa.ATIVA);
        Equipa beta = new Equipa(UUID.randomUUID(), "Beta", new Treinador(UUID.randomUUID(), "T2"), liga, EstadoEquipa.ATIVA);
        liga.adicionarEquipa(alfa);
        liga.adicionarEquipa(beta);

        // Falta a Beta na ordem.
        assertThrows(IllegalArgumentException.class,
                () -> classificacaoService.aplicarDesempate(liga, List.of(alfa.getId())));
    }

    /**
     * Um desempate resolvido para um empate a 10 pontos não pode decidir
     * sozinho um empate diferente e posterior, a 15 pontos, entre uma equipa
     * que lá estava e outra que nunca foi comparada com ela.
     */
    @Test
    public void umDesempateAntigoNaoDecideUmEmpatePosteriorDiferente() {
        ClassificacaoService classificacaoService = new ClassificacaoService();
        Gestor gestor = new Gestor(UUID.randomUUID(), "gestor@teste.pt", "hash", "Gestor");
        Liga liga = new Liga(UUID.randomUUID(), "Teste", 10, EstadoLiga.ATIVA, gestor);

        Equipa alfa = new Equipa(UUID.randomUUID(), "Alfa", new Treinador(UUID.randomUUID(), "T1"), liga, EstadoEquipa.ATIVA);
        Equipa zulu = new Equipa(UUID.randomUUID(), "Zulu", new Treinador(UUID.randomUUID(), "T2"), liga, EstadoEquipa.ATIVA);
        Equipa charlie = new Equipa(UUID.randomUUID(), "Charlie", new Treinador(UUID.randomUUID(), "T3"), liga, EstadoEquipa.ATIVA);
        liga.adicionarEquipa(alfa);
        liga.adicionarEquipa(zulu);
        liga.adicionarEquipa(charlie);

        DividaService dividaService = new DividaService(
                new com.ligarecord.repository.DividaRepositoryImpl(), new ClassificacaoService());
        JornadaService jornadaService = new JornadaService(
                new com.ligarecord.repository.JornadaRepositoryImpl(),
                new RegraDividaService(new com.ligarecord.repository.RegraDividaRepositoryImpl()),
                new CobrancaPeriodoService(new ClassificacaoService(), dividaService),
                dividaService);

        // Ronda 1: sem empates, só para dar avanço.
        Jornada r1 = jornadaService.abrirJornada(liga);
        jornadaService.inserirResultado(r1, alfa, 6);
        jornadaService.inserirResultado(r1, zulu, 3);
        jornadaService.inserirResultado(r1, charlie, 0);
        jornadaService.fecharJornada(r1);

        // Ronda 2: Alfa e Zulu empatam a 10 pontos (Charlie fica a 0). Resolvido
        // a favor de Zulu.
        Jornada r2 = jornadaService.abrirJornada(liga);
        jornadaService.inserirResultado(r2, alfa, 4);
        jornadaService.inserirResultado(r2, zulu, 7);
        jornadaService.inserirResultado(r2, charlie, 0);
        jornadaService.fecharJornada(r2);
        classificacaoService.aplicarDesempate(liga, List.of(zulu.getId(), alfa.getId()));

        // Ronda 3: Alfa dispara sozinha; Zulu e Charlie ficam empatados a 15 —
        // um empate diferente do da ronda 2, que nunca envolveu a Charlie, e
        // com Zulu já não nos mesmos pontos em que foi desempatada.
        Jornada r3 = jornadaService.abrirJornada(liga);
        jornadaService.inserirResultado(r3, alfa, 16);
        jornadaService.inserirResultado(r3, zulu, 5);
        jornadaService.inserirResultado(r3, charlie, 15);
        jornadaService.fecharJornada(r3);

        List<ClassificacaoGeral> classificacao = classificacaoService.calcularClassificacao(liga);

        assertEquals(alfa, classificacao.get(0).getEquipa());
        // Sem desempate resolvido para este empate, o nome decide — Charlie
        // antes de Zulu, não o contrário por causa de um valor de outra ronda.
        assertEquals(charlie, classificacao.get(1).getEquipa());
        assertEquals(zulu, classificacao.get(2).getEquipa());
    }

}
