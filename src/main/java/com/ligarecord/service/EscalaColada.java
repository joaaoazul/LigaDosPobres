package com.ligarecord.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lê a tabela de valores tal como o gestor a tem escrita.
 *
 * <p>Estas tabelas vivem numa nota do telemóvel, e é de lá que vêm — copiadas
 * e coladas. Pedir 21 campos separados era garantir que alguém se enganava a
 * transcrever, num sítio onde um engano custa dinheiro a alguém. Por isso o
 * que se aceita é o texto como ele é:
 *
 * <pre>
 * 1-0€
 * 2-0,10€
 * 3-0,30€
 * ...
 * 21- 2,50€
 * </pre>
 *
 * <p>Vírgula ou ponto decimal, com ou sem €, com ou sem espaços, e o separador
 * pode ser um hífen, dois pontos ou um travessão — tudo o que aparece nestas
 * notas. O que <b>não</b> se aceita é uma tabela com buracos ou fora de ordem:
 * aí não há maneira honesta de adivinhar o que o gestor queria dizer.
 */
public final class EscalaColada {

    /**
     * Posição, separador, valor. O travessão longo entra porque os telemóveis
     * trocam o hífen por ele sozinhos.
     */
    private static final Pattern LINHA =
            Pattern.compile("^\\s*(\\d{1,3})\\s*[-–—:.]\\s*(\\d+(?:[.,]\\d{1,2})?)\\s*(?:€|EUR|eur)?\\s*$");

    public static final int MAXIMO_POSICOES = 200;

    private EscalaColada() {
    }

    /**
     * Os valores por posição, do 1º em diante. A posição é a ordem na lista
     * devolvida — a coluna da esquerda serve só para confirmar que a tabela
     * está completa e por ordem.
     */
    public static List<BigDecimal> ler(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("A tabela de valores está vazia.");
        }

        List<BigDecimal> valores = new ArrayList<>();
        int esperada = 1;

        for (String linha : texto.split("\\R")) {
            if (linha.isBlank()) {
                continue;
            }

            Matcher encontrado = LINHA.matcher(linha);
            if (!encontrado.matches()) {
                throw new IllegalArgumentException(
                        "Não percebi esta linha da tabela: \"" + linha.trim() + "\". "
                                + "Cada linha é a posição, um hífen e o valor — por exemplo \"3-0,30€\".");
            }

            int posicao = Integer.parseInt(encontrado.group(1));
            if (posicao != esperada) {
                throw new IllegalArgumentException(
                        "A tabela tem de ir do 1º ao último, sem saltos: esperava a posição "
                                + esperada + " e encontrei a " + posicao + ".");
            }
            if (posicao > MAXIMO_POSICOES) {
                throw new IllegalArgumentException(
                        "A tabela não pode ter mais de " + MAXIMO_POSICOES + " posições.");
            }

            valores.add(new BigDecimal(encontrado.group(2).replace(',', '.')));
            esperada++;
        }

        if (valores.isEmpty()) {
            throw new IllegalArgumentException("A tabela de valores está vazia.");
        }
        return valores;
    }

    /** A tabela de volta a texto, para o gestor a reler e corrigir onde a escreveu. */
    public static String escrever(List<BigDecimal> valores) {
        StringBuilder texto = new StringBuilder();
        for (int i = 0; i < valores.size(); i++) {
            texto.append(i + 1).append('-')
                 .append(valores.get(i).toPlainString().replace('.', ','))
                 .append("€");
            if (i < valores.size() - 1) {
                texto.append('\n');
            }
        }
        return texto.toString();
    }
}
