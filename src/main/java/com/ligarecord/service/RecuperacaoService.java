package com.ligarecord.service;

import com.ligarecord.domain.Gestor;
import com.ligarecord.domain.PedidoRecuperacao;
import com.ligarecord.email.EnviadorDeEmail;
import com.ligarecord.email.ModeloDeEmail;
import com.ligarecord.repository.GestorRepository;
import com.ligarecord.repository.PedidoRecuperacaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

/**
 * Recuperação de password por email.
 *
 * <p>Três decisões que valem a pena guardar:
 *
 * <p><b>Pedir nunca diz se o email existe.</b> A resposta é a mesma para uma
 * conta que existe e para uma que não existe. Caso contrário este endereço
 * passava a ser uma forma de descobrir quem tem conta aqui, sem sequer precisar
 * de adivinhar uma password.
 *
 * <p><b>O código nunca é guardado.</b> Vai no email e o que fica na base de
 * dados é o resumo SHA-256. Um código destes dá uma conta que já existe, com as
 * ligas e o dinheiro lá dentro, e não faz sentido que exista em texto em dois
 * sítios.
 *
 * <p><b>Redefinir corta as sessões abertas.</b> Quem recupera a password
 * costuma fazê-lo porque desconfia que alguém entrou. Se as sessões dessa
 * pessoa continuassem vivas, a recuperação não servia para nada.
 */
@Service
public class RecuperacaoService {

    private static final Logger LOG = LoggerFactory.getLogger(RecuperacaoService.class);

    /** Uma hora: tempo de sobra para quem foi ver o email, e curto para o resto. */
    static final Duration VALIDADE = Duration.ofHours(1);

    /** Travão ao envio repetido, para ninguém encher a caixa de correio de outra pessoa. */
    static final Duration JANELA_LIMITE = Duration.ofHours(1);
    static final int MAXIMO_POR_JANELA = 3;

    private final GestorRepository gestorRepository;
    private final PedidoRecuperacaoRepository pedidoRepository;
    private final PasswordEncoder passwordEncoder;
    private final EnviadorDeEmail email;
    private final String base;

    public RecuperacaoService(GestorRepository gestorRepository,
                              PedidoRecuperacaoRepository pedidoRepository,
                              PasswordEncoder passwordEncoder,
                              EnviadorDeEmail email,
                              @Value("${app.url:http://localhost:8080}") String base) {
        this.gestorRepository = gestorRepository;
        this.pedidoRepository = pedidoRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.base = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    /**
     * Cria um pedido e envia o link, se houver conta ativa com este email.
     *
     * <p>Não devolve nada e não se queixa de nada: quem chama isto responde
     * sempre o mesmo ao utilizador, exista a conta ou não.
     */
    @Transactional
    public void pedir(String emailPedido) {
        String normalizado;
        try {
            normalizado = RegrasDeConta.emailNormalizado(emailPedido);
        } catch (IllegalArgumentException e) {
            // Um email mal escrito não é conta nenhuma. Cala-se, como no resto.
            return;
        }

        Optional<Gestor> encontrado = gestorRepository.buscarPorEmail(normalizado);
        if (encontrado.isEmpty() || !encontrado.get().isAtivo()) {
            return;
        }
        Gestor gestor = encontrado.get();

        Instant desde = Instant.now().minus(JANELA_LIMITE);
        if (pedidoRepository.contarDoGestorDesde(gestor.getId(), desde) >= MAXIMO_POR_JANELA) {
            LOG.warn("Limite de pedidos de recuperação atingido para a conta {}.", gestor.getId());
            return;
        }

        String codigo = CodigosAleatorios.gerar();
        pedidoRepository.guardar(new PedidoRecuperacao(
                UUID.randomUUID(), resumo(codigo), gestor, Instant.now().plus(VALIDADE)));

        ModeloDeEmail.Mensagem mensagem = ModeloDeEmail.recuperacaoDePassword(
                gestor.getNome(), link(codigo), validadePorExtenso());
        email.enviar(gestor.getEmail(), mensagem.assunto(), mensagem.texto(), mensagem.html());
    }

    /**
     * Redefine a password a partir de um código válido, que fica consumido.
     *
     * <p>A mensagem de erro é a mesma para um código inventado, um já usado e um
     * expirado: quem tem o link legítimo não precisa da distinção, e quem anda a
     * tentar códigos não fica a saber que acertou num que já existiu.
     */
    @Transactional
    public void redefinir(String codigo, String novaPassword) {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("O link de recuperação não é válido ou já expirou.");
        }

        PedidoRecuperacao pedido = pedidoRepository.buscarPorCodigoHash(resumo(codigo))
                .filter(PedidoRecuperacao::estaDisponivel)
                .orElseThrow(() -> new IllegalArgumentException(
                        "O link de recuperação não é válido ou já expirou."));

        RegrasDeConta.validarPasswordNova(novaPassword);

        Gestor gestor = pedido.getGestor();
        gestor.setPasswordHash(passwordEncoder.encode(novaPassword));
        gestor.invalidarSessoesAbertas();
        gestorRepository.guardar(gestor);

        pedido.marcarUsado();
        pedidoRepository.guardar(pedido);
    }

    private String link(String codigo) {
        return base + "/nova-password.html?codigo=" + codigo;
    }

    /**
     * A validade dita por extenso, a partir da constante. Escrita à mão, ficava
     * a dizer "uma hora" no dia em que alguém mudasse a {@link #VALIDADE} e se
     * esquecesse de vir aqui.
     */
    private String validadePorExtenso() {
        long horas = VALIDADE.toHours();
        if (horas == 1) {
            return "dentro de uma hora";
        }
        return horas > 0
                ? "dentro de " + horas + " horas"
                : "dentro de " + VALIDADE.toMinutes() + " minutos";
    }

    /**
     * SHA-256 e não BCrypt: aqui não há nada a proteger de força bruta, porque o
     * código tem 24 bytes aleatórios e não é uma password que alguém escolheu.
     * O BCrypt, sendo lento de propósito, também impedia a procura pelo resumo,
     * que é como o pedido é encontrado.
     */
    private String resumo(String codigo) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(sha.digest(codigo.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 tem de existir em qualquer JVM.", e);
        }
    }
}
