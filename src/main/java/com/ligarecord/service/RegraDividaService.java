package com.ligarecord.service;

import com.ligarecord.domain.Liga;
import com.ligarecord.domain.RegraDivida;
import com.ligarecord.domain.enums.EscalaDivida;
import com.ligarecord.repository.RegraDividaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * A regra de cobrança de uma liga: quanto custa entrar, e como o valor por
 * período sobe por escalão de classificação. Uma liga sem regra não tem
 * cobrança automática — ver {@link DividaService#prontoParaFecharBloco}.
 */
@Service
public class RegraDividaService {

    private final RegraDividaRepository regraDividaRepository;

    public RegraDividaService(RegraDividaRepository regraDividaRepository) {
        this.regraDividaRepository = regraDividaRepository;
    }

    /**
     * Cria ou substitui a regra da liga, na escala de fórmula. Idempotente de
     * propósito: o gestor pode ajustar valores a meio da época sem ter de
     * saber se já existia uma regra antes.
     */
    @Transactional
    public RegraDivida definir(Liga liga, BigDecimal valorInscricao, BigDecimal valorInicial,
                              BigDecimal incremento, int equipasPorEscalao, BigDecimal valorMaximo,
                              int jornadasPorBloco) {
        return definir(liga, valorInscricao, valorInicial, incremento, equipasPorEscalao,
                valorMaximo, jornadasPorBloco, EscalaDivida.FORMULA, null, true, null);
    }

    /**
     * O mesmo, dizendo de onde sai o valor de cada posição e se as jornadas de
     * treino são cobradas.
     *
     * <p>Numa regra por tabela os campos da fórmula continuam a ser guardados —
     * não servem para nada enquanto a tabela estiver em vigor, mas ficam lá
     * para quem voltar atrás não ter de os reescrever.
     */
    @Transactional
    public RegraDivida definir(Liga liga, BigDecimal valorInscricao, BigDecimal valorInicial,
                              BigDecimal incremento, int equipasPorEscalao, BigDecimal valorMaximo,
                              int jornadasPorBloco, EscalaDivida escala,
                              List<BigDecimal> tabela, boolean cobraTreino,
                              List<RegraDivida.CobrancaPedida> cobrancas) {
        if (liga == null) {
            throw new IllegalArgumentException("A liga é obrigatória.");
        }
        if (escala == null) {
            throw new IllegalArgumentException("É preciso dizer se a escala é fórmula ou tabela.");
        }
        if (escala == EscalaDivida.TABELA) {
            if (tabela == null || tabela.isEmpty()) {
                throw new IllegalArgumentException("Uma regra por tabela precisa da tabela de valores.");
            }
            for (BigDecimal valor : tabela) {
                exigirNaoNegativo(valor, "Um valor da tabela");
            }
        }
        if (cobrancas != null) {
            for (RegraDivida.CobrancaPedida cobranca : cobrancas) {
                // 40 é o que a coluna aguenta, e é o que aparece na lista de
                // dívidas ao lado do valor — mais do que isso não caberia lá.
                RegrasDeConta.textoValidado(cobranca.nome(), 40, "O nome da cobrança");
                if (cobranca.jornadaOficial() < 1) {
                    throw new IllegalArgumentException(
                            "A jornada da cobrança \"" + cobranca.nome() + "\" tem de ser pelo menos 1.");
                }
                if (cobranca.tabela() == null || cobranca.tabela().isEmpty()) {
                    throw new IllegalArgumentException(
                            "A cobrança \"" + cobranca.nome() + "\" precisa da tabela de valores.");
                }
            }
            long nomesDistintos = cobrancas.stream()
                    .map(cobranca -> cobranca.nome().trim().toLowerCase())
                    .distinct().count();
            if (nomesDistintos != cobrancas.size()) {
                throw new IllegalArgumentException("Duas cobranças não podem ter o mesmo nome.");
            }
        }
        exigirNaoNegativo(valorInscricao, "O valor de inscrição");
        exigirNaoNegativo(valorInicial, "O valor inicial");
        exigirNaoNegativo(incremento, "O incremento");
        exigirNaoNegativo(valorMaximo, "O valor máximo");
        if (equipasPorEscalao < 1) {
            throw new IllegalArgumentException("O número de equipas por escalão tem de ser pelo menos 1.");
        }
        if (jornadasPorBloco < 1) {
            throw new IllegalArgumentException("O número de jornadas por bloco tem de ser pelo menos 1.");
        }
        if (valorMaximo.compareTo(valorInicial) < 0) {
            throw new IllegalArgumentException("O valor máximo não pode ser inferior ao valor inicial.");
        }

        RegraDivida regra = regraDividaRepository.buscarPorLiga(liga).orElse(null);
        if (regra == null) {
            regra = new RegraDivida(UUID.randomUUID(), liga, valorInscricao, valorInicial,
                    incremento, equipasPorEscalao, valorMaximo, jornadasPorBloco);
        } else {
            regra.setValorInscricao(valorInscricao);
            regra.setValorInicial(valorInicial);
            regra.setIncremento(incremento);
            regra.setEquipasPorEscalao(equipasPorEscalao);
            regra.setValorMaximo(valorMaximo);
            regra.setJornadasPorBloco(jornadasPorBloco);
        }

        regra.setEscala(escala);
        regra.setCobraTreino(cobraTreino);
        if (escala == EscalaDivida.TABELA) {
            regra.substituirTabela(tabela);
        }
        // A null deixa as cobranças como estão: um cliente que só quer mexer nos
        // valores não tem de reenviar o Inverno e o Verão para não os perder.
        if (cobrancas != null) {
            regra.acertarCobrancas(cobrancas);
        }

        return regraDividaRepository.guardar(regra);
    }

    @Transactional(readOnly = true)
    public Optional<RegraDivida> buscarPorLiga(Liga liga) {
        return regraDividaRepository.buscarPorLiga(liga);
    }

    private void exigirNaoNegativo(BigDecimal valor, String campo) {
        if (valor == null || valor.signum() < 0) {
            throw new IllegalArgumentException(campo + " não pode ser negativo.");
        }
    }
}
