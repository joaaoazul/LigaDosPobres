package com.ligarecord.service;

import com.ligarecord.domain.BlocoDivida;
import com.ligarecord.domain.Divida;
import com.ligarecord.domain.Equipa;
import com.ligarecord.domain.Gestor;
import com.ligarecord.domain.Liga;
import com.ligarecord.domain.enums.EstadoDivida;
import com.ligarecord.repository.DividaRepository;
import com.ligarecord.web.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Dívidas de equipas, acumuladas em blocos. O valor de cada bloco é hoje
 * escrito à mão pelo gestor ao fechá-lo — não há (ainda) cálculo automático a
 * partir da classificação da liga.
 */
@Service
public class DividaService {

    private final DividaRepository dividaRepository;

    public DividaService(DividaRepository dividaRepository) {
        this.dividaRepository = dividaRepository;
    }

    /**
     * Acrescenta um novo bloco à dívida da equipa, criando a dívida se for o
     * primeiro bloco desta equipa.
     */
    @Transactional
    public BlocoDivida registarBloco(Equipa equipa, BigDecimal valor) {
        if (valor == null || valor.signum() < 0) {
            throw new IllegalArgumentException("O valor do bloco não pode ser negativo.");
        }

        Divida divida = dividaRepository.buscarPorEquipa(equipa)
                .orElseGet(() -> new Divida(UUID.randomUUID(), equipa, EstadoDivida.PENDENTE));

        BlocoDivida bloco = divida.registarBloco(valor);
        dividaRepository.guardarDivida(divida);
        return bloco;
    }

    /**
     * Marca um bloco da dívida da equipa como pago. Só o gestor faz isto — não
     * há confirmação nem acção do lado do treinador.
     */
    @Transactional
    public void resolverBloco(Equipa equipa, UUID blocoId) {
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
        dividaRepository.guardarDivida(divida);
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

    /** As dívidas de todas as equipas que esta conta treina, por equipa e liga. */
    @Transactional(readOnly = true)
    public List<Divida> listarPorTreinador(Gestor conta) {
        return dividaRepository.buscarPorTreinador(conta);
    }

    @Transactional(readOnly = true)
    public List<Divida> listarPorLiga(Liga liga, EstadoDivida estado) {
        return dividaRepository.listarDividas(liga, estado);
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
