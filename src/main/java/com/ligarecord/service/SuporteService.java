package com.ligarecord.service;

import com.ligarecord.domain.MensagemSuporte;
import com.ligarecord.email.EnviadorDeEmail;
import com.ligarecord.email.ModeloDeEmail;
import com.ligarecord.repository.MensagemSuporteRepository;
import com.ligarecord.web.PedidosDemaisException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * O formulário de contacto da página de entrada.
 *
 * <p>É o único sítio da aplicação onde alguém sem conta nenhuma faz sair um
 * email — e é de propósito: serve exactamente para quem não consegue entrar.
 * Isso obriga a três cuidados que os outros serviços não precisam de ter.
 *
 * <p><b>A mensagem é guardada antes de ser enviada.</b> Se o Resend falhar (ou
 * faltar a chave), ela fica na base de dados em vez de desaparecer entre o
 * "enviar" e o nada. Quem escreveu não tem como saber que o envio falhou nem
 * como voltar a tentar com o mesmo texto.
 *
 * <p><b>Há um travão por origem.</b> Sem ele, um guião qualquer enchia a caixa
 * de correio do suporte e a conta do Resend em minutos. Conta-se por IP e não
 * pelo email escrito no formulário: o email é texto que quem envia escolhe, e
 * mudá-lo a cada pedido não custa nada. O travão não é à prova de quem se dê ao
 * trabalho de mudar de origem — é para o que aparece sozinho.
 *
 * <p><b>Falhar o envio não é erro de quem escreveu.</b> A mensagem já ficou
 * guardada, e mandar abaixo o pedido convidava a pessoa a reescrever tudo e a
 * gastar o travão outra vez para nada.
 */
@Service
public class SuporteService {

    private static final Logger LOG = LoggerFactory.getLogger(SuporteService.class);

    /** Mais do que isto, da mesma origem em uma hora, já não é gente a pedir ajuda. */
    static final Duration JANELA_LIMITE = Duration.ofHours(1);
    static final int MAXIMO_POR_JANELA = 5;

    static final int MAXIMO_ASSUNTO = 120;
    static final int MAXIMO_MENSAGEM = 4000;
    /** 45 é o que uma morada IPv6 ocupa por extenso, e é o que a coluna aguenta. */
    private static final int MAXIMO_IP = 45;

    private final MensagemSuporteRepository mensagens;
    private final EnviadorDeEmail email;
    private final String destino;

    public SuporteService(MensagemSuporteRepository mensagens,
                          EnviadorDeEmail email,
                          @Value("${app.suporte.email:}") String suporte,
                          @Value("${app.admin.email:}") String admin) {
        this.mensagens = mensagens;
        this.email = email;
        // Sem endereço de suporte configurado, vai para o do administrador: numa
        // instalação pequena são a mesma pessoa, e obrigar a configurar duas
        // variáveis para o mesmo endereço só dava um formulário calado.
        this.destino = suporte.isBlank() ? admin.trim() : suporte.trim();
    }

    /**
     * Valida, guarda e tenta enviar. Devolve se o email chegou a sair — quem
     * chama usa isso só para escolher o que dizer a quem escreveu, porque a
     * mensagem ficou registada de qualquer maneira.
     */
    @Transactional
    public boolean receber(String nome, String emailDeQuemEscreve, String assunto,
                           String mensagem, String ip) {
        String nomeValidado = RegrasDeConta.nomeValidado(nome);
        String emailValidado = RegrasDeConta.emailNormalizado(emailDeQuemEscreve);
        String assuntoValidado = RegrasDeConta.textoValidado(assunto, MAXIMO_ASSUNTO, "O assunto");
        String textoValidado = RegrasDeConta.textoValidado(mensagem, MAXIMO_MENSAGEM, "A mensagem");
        String origem = origemNormalizada(ip);

        Instant desde = Instant.now().minus(JANELA_LIMITE);
        if (origem != null && mensagens.contarDoIpDesde(origem, desde) >= MAXIMO_POR_JANELA) {
            LOG.warn("Limite de mensagens de suporte atingido para a origem {}.", origem);
            throw new PedidosDemaisException(
                    "Já enviaste várias mensagens há pouco. Espera um bocado antes de enviares outra.");
        }

        mensagens.guardar(new MensagemSuporte(UUID.randomUUID(), nomeValidado, emailValidado,
                assuntoValidado, textoValidado, origem));

        if (destino.isBlank()) {
            LOG.warn("Mensagem de suporte guardada mas sem destino configurado "
                    + "(SUPORTE_EMAIL ou ADMIN_EMAIL): ninguém foi avisado por email.");
            return false;
        }

        ModeloDeEmail.Mensagem aviso = ModeloDeEmail.mensagemDeSuporte(
                nomeValidado, emailValidado, assuntoValidado, textoValidado);
        return email.enviar(destino, aviso.assunto(), aviso.texto(), aviso.html(), emailValidado);
    }

    /**
     * O IP como ele pode ser guardado: cortado ao tamanho da coluna, e a null
     * se vier vazio. Cortar em vez de recusar porque a origem é um detalhe do
     * pedido, não algo que quem escreveu controle — recusar a mensagem por
     * causa dela era perder uma mensagem verdadeira por um cabeçalho estranho.
     */
    private String origemNormalizada(String ip) {
        if (ip == null || ip.isBlank()) {
            return null;
        }
        String limpo = ip.trim();
        return limpo.length() > MAXIMO_IP ? limpo.substring(0, MAXIMO_IP) : limpo;
    }
}
