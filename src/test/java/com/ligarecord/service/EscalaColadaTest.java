package com.ligarecord.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** O texto que o gestor cola vem de uma nota do telemóvel, com tudo o que isso traz. */
class EscalaColadaTest {

    @Test
    void leATabelaTalComoEstaEscrita() {
        List<BigDecimal> valores = EscalaColada.ler("""
                1-0€
                2-0,10€
                3-0,30€
                """);

        assertEquals(3, valores.size());
        assertEquals(0, valores.get(0).compareTo(new BigDecimal("0")));
        assertEquals(0, valores.get(1).compareTo(new BigDecimal("0.10")));
        assertEquals(0, valores.get(2).compareTo(new BigDecimal("0.30")));
    }

    /** Ponto ou vírgula, com ou sem €, com ou sem espaços, travessão do telemóvel. */
    @Test
    void aguentaAsFormasQueAparecemNumaNota() {
        List<BigDecimal> valores = EscalaColada.ler("""
                1 - 0
                2-0.50€
                3–1,5 €
                4: 2,00EUR
                """);

        assertEquals(List.of("0", "0.50", "1.5", "2.00"),
                valores.stream().map(BigDecimal::toPlainString).toList());
    }

    @Test
    void ignoraLinhasEmBranco() {
        assertEquals(2, EscalaColada.ler("\n1-0€\n\n2-0,10€\n\n").size());
    }

    /**
     * Um salto na tabela não se adivinha: não dá para saber se a posição que
     * falta é de graça, se é igual à anterior, ou se foi esquecimento.
     */
    @Test
    void recusaUmaTabelaComSaltos() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> EscalaColada.ler("1-0€\n3-0,30€"));

        assertTrue(erro.getMessage().contains("sem saltos"));
    }

    @Test
    void recusaUmaTabelaForaDeOrdem() {
        assertThrows(IllegalArgumentException.class, () -> EscalaColada.ler("2-0,10€\n1-0€"));
    }

    @Test
    void recusaLinhasQueNaoPercebe() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> EscalaColada.ler("1-0€\nsegundo lugar paga dez cêntimos"));

        // A mensagem diz qual foi a linha: numa tabela de 21 é a diferença
        // entre corrigir e reescrever tudo.
        assertTrue(erro.getMessage().contains("segundo lugar paga dez cêntimos"));
    }

    @Test
    void recusaUmaTabelaVazia() {
        assertThrows(IllegalArgumentException.class, () -> EscalaColada.ler(""));
        assertThrows(IllegalArgumentException.class, () -> EscalaColada.ler(null));
        assertThrows(IllegalArgumentException.class, () -> EscalaColada.ler("\n\n"));
    }

    /** Ida e volta: o que se lê tem de voltar a sair na forma em que entrou. */
    @Test
    void escreverEVoltarALerDaOMesmo() {
        String texto = "1-0€\n2-0,10€\n3-0,30€";

        assertEquals(texto, EscalaColada.escrever(EscalaColada.ler(texto)));
    }
}
