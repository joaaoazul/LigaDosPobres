package com.ligarecord.service;

import com.ligarecord.domain.BlocoDivida;
import com.ligarecord.domain.Divida;
import com.ligarecord.domain.Equipa;
import com.ligarecord.domain.Gestor;
import com.ligarecord.domain.Jornada;
import com.ligarecord.domain.Liga;
import com.ligarecord.domain.RegraDivida;
import com.ligarecord.domain.ResultadoJornada;
import com.ligarecord.domain.enums.EstadoDivida;
import com.ligarecord.domain.enums.EstadoEquipa;
import com.ligarecord.domain.enums.EstadoJornada;
import com.ligarecord.repository.DividaRepository;
import com.ligarecord.web.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Dívidas de equipas, acumuladas em blocos. Sem {@link RegraDivida} definida
 * para a liga, o valor de cada bloco é escrito à mão pelo gestor; com regra,
 * fechar jornadas suficientes fecha um bloco automaticamente, com o valor
 * calculado por escalão de classificação (ver {@link ClassificacaoService}).
 */
@Service
public class DividaService {

    private final DividaRepository dividaRepository;
    private final ClassificacaoService classificacaoService;

    public DividaService(DividaRepository dividaRepository, ClassificacaoService classificacaoService) {
        this.dividaRepository = dividaRepository;
        this.classificacaoService = classificacaoService;
    }

    /**
     * Acrescenta um novo bloco de período à dívida da equipa, criando a
     * dívida se for o primeiro bloco desta equipa.
     */
    @Transactional
    public BlocoDivida registarBloco(Equipa equipa, BigDecimal valor) {
        if (valor == null || valor.signum() < 0) {
            throw new IllegalArgumentException("O valor do bloco não pode ser negativo.");
        }

        Divida divida = dividaOuNova(equipa);
        BlocoDivida bloco = divida.registarBloco(valor);
        dividaRepository.guardarDivida(divida);
        return bloco;
    }

    /**
     * Cobra a inscrição da equipa na liga — uma vez, tipicamente ao ser
     * adicionada. Isolada dos blocos de período (ver {@link BlocoDivida}),
     * mas soma para o mesmo total em dívida da equipa.
     */
    @Transactional
    public BlocoDivida registarInscricao(Equipa equipa, BigDecimal valor) {
        if (valor == null || valor.signum() < 0) {
            throw new IllegalArgumentException("O valor da inscrição não pode ser negativo.");
        }

        Divida divida = dividaOuNova(equipa);
        BlocoDivida bloco = divida.registarInscricao(valor);
        dividaRepository.guardarDivida(divida);
        return bloco;
    }

    /**
     * Diz se fechar esta jornada fecha também um bloco de período, segundo a
     * periodicidade da regra da liga. Sem regra definida, nunca — a liga não
     * tem cobrança automática. Chamado depois de a jornada já estar fechada.
     */
    @Transactional(readOnly = true)
    public boolean jornadaFechaBloco(Jornada jornada, RegraDivida regra) {
        if (regra == null) {
            return false;
        }
        long fechadas = jornada.getLiga().getJornadas().stream()
                .filter(j -> j.getEstadoJ() == EstadoJornada.FECHADA)
                .count();
        return fechadas % regra.getJornadasPorBloco() == 0;
    }

    /**
     * Fecha um bloco de período para a liga inteira: cada jornada do bloco
     * tem a sua própria classificação e o seu próprio valor por escalão —
     * não é a classificação geral acumulada desde o início da liga. O que
     * cada equipa ainda ativa paga no bloco é a soma dos valores de cada uma
     * das jornadas que o compõem. Equipas desistentes não voltam a ser
     * cobradas.
     */
    @Transactional
    public void processarFechoBloco(Liga liga, RegraDivida regra, List<Jornada> jornadasDoBloco) {
        Map<Equipa, BigDecimal> totalPorEquipa = new HashMap<>();
        for (Jornada jornada : jornadasDoBloco) {
            for (ResultadoJornada resultado : jornada.getResultadoJ()) {
                Equipa equipa = resultado.getEquipa();
                if (equipa.getEstado() != EstadoEquipa.ATIVA) {
                    continue;
                }
                BigDecimal valor = classificacaoService.calcularValor(regra, resultado.getPosicao());
                totalPorEquipa.merge(equipa, valor, BigDecimal::add);
            }
        }
        totalPorEquipa.forEach(this::registarBloco);
    }

    /**
     * Marca um bloco da dívida da equipa como pago. Só o gestor faz isto — não
     * há confirmação nem acção do lado do treinador.
     */
    @Transactional
    public Divida resolverBloco(Equipa equipa, UUID blocoId) {
        Divida divida = buscarDividaOuFalhar(equipa);
        BlocoDivida bloco = divida.getBlocos().stream()
                .filter(b -> b.getId().equals(blocoId))
                .findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("Bloco não encontrado."));

        if (bloco.estaResolvido()) {
            throw new IllegalStateException("Este bloco já estava pago.");
        }

        bloco.marcarResolvido();
        atualizarEstado(divida);
        return dividaRepository.guardarDivida(divida);
    }

    /** Marca todos os blocos ainda pendentes da equipa como pagos de uma vez. */
    @Transactional
    public Divida resolverDivida(Equipa equipa) {
        Divida divida = buscarDividaOuFalhar(equipa);

        divida.getBlocos().stream()
                .filter(bloco -> !bloco.estaResolvido())
                .forEach(BlocoDivida::marcarResolvido);
        divida.setEstado(EstadoDivida.RESOLVIDA);

        return dividaRepository.guardarDivida(divida);
    }

    @Transactional(readOnly = true)
    public BigDecimal calcularTotalDivida(Divida divida) {
        return divida.getBlocos().stream()
                .filter(bloco -> !bloco.estaResolvido())
                .map(BlocoDivida::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Soma tudo o que as equipas desta conta (como treinador) ainda devem, em qualquer liga. */
    @Transactional(readOnly = true)
    public BigDecimal calcularTotalTreinador(Gestor conta) {
        return listarPorTreinador(conta).stream()
                .map(this::calcularTotalDivida)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * As dívidas já registadas (com pelo menos um bloco) das equipas que esta
     * conta treina. Uma equipa sem nenhum bloco ainda não aparece aqui — para
     * a lista completa, incluindo essas, ver como {@code MinhasDividasController}
     * percorre antes o Treinador da conta, não a Divida.
     */
    @Transactional(readOnly = true)
    public List<Divida> listarPorTreinador(Gestor conta) {
        return dividaRepository.buscarPorTreinador(conta);
    }

    @Transactional(readOnly = true)
    public List<Divida> listarPorLiga(Liga liga, EstadoDivida estado) {
        return dividaRepository.listarDividas(liga, estado);
    }

    @Transactional(readOnly = true)
    public Optional<Divida> buscarPorEquipa(Equipa equipa) {
        return dividaRepository.buscarPorEquipa(equipa);
    }

    private Divida dividaOuNova(Equipa equipa) {
        return dividaRepository.buscarPorEquipa(equipa)
                .orElseGet(() -> new Divida(UUID.randomUUID(), equipa, EstadoDivida.PENDENTE));
    }

    private Divida buscarDividaOuFalhar(Equipa equipa) {
        return dividaRepository.buscarPorEquipa(equipa)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Esta equipa não tem dívida registada."));
    }

    private void atualizarEstado(Divida divida) {
        boolean aindaDeve = divida.getBlocos().stream().anyMatch(bloco -> !bloco.estaResolvido());
        divida.setEstado(aindaDeve ? EstadoDivida.PENDENTE : EstadoDivida.RESOLVIDA);
    }
}
