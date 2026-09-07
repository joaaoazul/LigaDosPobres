package com.ligarecord.service;

import com.ligarecord.domain.ConviteTreinador;
import com.ligarecord.domain.Equipa;
import com.ligarecord.domain.Gestor;
import com.ligarecord.domain.Liga;
import com.ligarecord.domain.Treinador;
import com.ligarecord.email.EnviadorDeEmail;
import com.ligarecord.email.ModeloDeEmail;
import com.ligarecord.repository.ConviteTreinadorRepository;
import com.ligarecord.web.ConviteInvalidoException;
import com.ligarecord.web.RecursoNaoEncontradoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Convites que ligam uma conta ao lugar de treinador de uma equipa.
 *
 * <p>Distinto do {@link ConviteService}, que serve para criar contas de gestor:
 * ali o convite é aberto — quem o tiver escolhe quem é — e só um administrador o
 * cria; aqui o convite já nasce apontado a uma equipa concreta, e é o gestor
 * dono da liga que o emite. Partilhar uma só classe obrigaria a campos opcionais
 * que metade dos convites nunca usaria, e a validações que só se aplicam a
 * alguns; separados, cada um diz exactamente o que é.
 */
@Service
public class ConviteTreinadorService {

    private static final int VALIDADE_MAXIMA_DIAS = 365;

    /**
     * Um convite é uma credencial, e uma credencial sem prazo anda em conversas
     * de WhatsApp para sempre. Trinta dias chegam de sobra para quem vai
     * aceitar, e quem perder o prazo pede outro — que é um clique.
     */
    static final int VALIDADE_OMISSAO_DIAS = 30;

    /**
     * Travão ao envio repetido. Ao contrário da recuperação de password, onde
     * a janela é por conta e por hora, aqui conta-se por convite: um convite
     * pertence a um lugar e dura trinta dias, por isso o que interessa travar
     * não é a pressa de um dia, é a insistência ao longo do prazo todo.
     *
     * <p>Três chegam para o primeiro envio e dois lembretes. Quem precisar de
     * mais entrega o link à mão — ele vem sempre na resposta. Revogar e emitir
     * de novo contorna isto, e é deliberado: dá outra credencial, deixa rasto,
     * e não é coisa que se faça sem dar por ela.
     */
    static final int MAXIMO_ENVIOS = 3;

    /** E não dois seguidos por engano, com dois cliques no mesmo botão. */
    static final Duration INTERVALO_MINIMO = Duration.ofMinutes(10);

    private static final Logger LOG = LoggerFactory.getLogger(ConviteTreinadorService.class);

    /** O prazo é dito por extenso a quem recebe; a hora certa não interessa a ninguém. */
    private static final DateTimeFormatter DATA =
            DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.of("pt", "PT"))
                    .withZone(ZoneId.of("Europe/Lisbon"));

    private final ConviteTreinadorRepository conviteRepository;
    private final EnviadorDeEmail email;
    private final LinksDaAplicacao links;

    public ConviteTreinadorService(ConviteTreinadorRepository conviteRepository,
                                   EnviadorDeEmail email,
                                   LinksDaAplicacao links) {
        this.conviteRepository = conviteRepository;
        this.email = email;
        this.links = links;
    }

    /** O que aconteceu ao tentar entregar o convite por email. */
    public enum Envio {
        /** Saiu. */
        ENVIADO,
        /** O lugar não tem email: o gestor entrega o link como quiser. */
        SEM_EMAIL,
        /** Já foram enviados os que se permitem, ou foi há pouco tempo. */
        LIMITE_ATINGIDO,
        /** Havia email e tentou-se, mas o envio não saiu. */
        FALHOU
    }

    /**
     * O convite e se ele foi criado agora ou já existia — o suficiente para
     * quem chama responder {@code 201} ou {@code 200} sem ter de adivinhar.
     */
    public record Emissao(ConviteTreinador convite, boolean novo) {
    }

    /**
     * Emite o convite para o lugar de treinador desta equipa, ou devolve o que
     * já lá estiver por usar.
     *
     * <p><b>Não cria um segundo convite para o mesmo lugar.</b> Carregar duas
     * vezes no botão espalhava duas credenciais válidas para a mesma coisa, e a
     * primeira ficava a valer sem ninguém saber onde parava. Quem quiser
     * invalidar o que já deu, revoga-o e emite outro.
     *
     * <p>Quem chama tem de ter resolvido a equipa por {@code buscarPorIdEGestor}
     * — é essa consulta que prova que o gestor manda na liga da equipa. Aqui
     * verifica-se apenas o que essa consulta não pode saber: que o lugar ainda
     * não tem conta ligada.
     */
    @Transactional
    public Emissao emitir(Gestor criadoPor, Equipa equipa, Integer diasValidade) {
        if (criadoPor == null) {
            throw new IllegalArgumentException("O convite tem de ter um autor.");
        }
        if (equipa == null) {
            throw new IllegalArgumentException("O convite tem de ter uma equipa.");
        }
        if (diasValidade != null && (diasValidade < 1 || diasValidade > VALIDADE_MAXIMA_DIAS)) {
            throw new IllegalArgumentException(
                    "A validade tem de estar entre 1 e " + VALIDADE_MAXIMA_DIAS + " dias.");
        }

        Treinador treinador = equipa.getTreinador();
        if (treinador.temConta()) {
            throw new IllegalStateException("Este treinador já tem conta.");
        }

        Optional<ConviteTreinador> pendente = disponivelDoTreinador(treinador.getId());
        if (pendente.isPresent()) {
            return new Emissao(pendente.get(), false);
        }

        int dias = diasValidade == null ? VALIDADE_OMISSAO_DIAS : diasValidade;
        ConviteTreinador convite = conviteRepository.guardar(new ConviteTreinador(
                UUID.randomUUID(),
                CodigosAleatorios.gerar(),
                equipa,
                criadoPor,
                Instant.now().plus(dias, ChronoUnit.DAYS)
        ));
        return new Emissao(convite, true);
    }

    /**
     * Entrega o convite por email, a partir do id.
     *
     * <p>É por id e não pela entidade porque o email sai sempre <em>depois</em>
     * de o convite estar gravado, já noutra transação — e as entidades da
     * anterior não sobrevivem ao commit.
     *
     * <p>Falhar a enviar não desfaz o convite, que continua a valer pelo link.
     * Devolve o que aconteceu para quem carregou no botão poder ser informado:
     * foi ele que escreveu o endereço, e tem direito a saber se aquilo chegou
     * a partir.
     */
    @Transactional
    public Envio enviarPorEmail(UUID conviteId) {
        return enviar(conviteRepository.buscarPorId(conviteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Convite não encontrado.")));
    }

    private Envio enviar(ConviteTreinador convite) {
        if (!convite.estaDisponivel()) {
            throw new ConviteInvalidoException("Código de convite inválido.");
        }

        Treinador treinador = convite.getTreinador();
        if (!treinador.temEmail()) {
            return Envio.SEM_EMAIL;
        }
        if (!podeEnviar(convite)) {
            LOG.warn("Travado o envio repetido do convite {} ({} envios).",
                    convite.getId(), convite.getEnvios());
            return Envio.LIMITE_ATINGIDO;
        }

        Liga liga = convite.getEquipa().getLiga();
        ModeloDeEmail.Mensagem mensagem = ModeloDeEmail.conviteDeTreinador(
                treinador.getNome(),
                convite.getCriadoPor().getNome(),
                convite.getEquipa().getNome(),
                liga == null ? null : liga.getNome(),
                links.convite(convite.getCodigo()),
                validadePorExtenso(convite));

        boolean saiu = email.enviar(treinador.getEmail(), mensagem.assunto(),
                mensagem.texto(), mensagem.html());
        if (!saiu) {
            return Envio.FALHOU;
        }

        // Só se conta o que saiu: uma tentativa falhada não pode gastar o
        // travão, ou uma falha do servidor de email deixava o gestor sem
        // maneira de tentar outra vez.
        convite.marcarEnviado(treinador.getEmail());
        conviteRepository.guardar(convite);
        return Envio.ENVIADO;
    }

    /**
     * A V11 pôs prazo em todos os convites vivos que não tinham, e emitir passou
     * a pôr sempre um. Isto continua a contar com o nulo à mesma: era uma
     * mensagem inteira por enviar — e um erro no ecrã de quem carregou no
     * botão — por causa de uma data que faltava.
     */
    private String validadePorExtenso(ConviteTreinador convite) {
        return convite.getExpiraEm() == null
                ? "só quando for usado"
                : "a " + DATA.format(convite.getExpiraEm());
    }

    private boolean podeEnviar(ConviteTreinador convite) {
        if (convite.getEnvios() >= MAXIMO_ENVIOS) {
            return false;
        }
        Instant ultimo = convite.getEnviadoEm();
        return ultimo == null || Instant.now().isAfter(ultimo.plus(INTERVALO_MINIMO));
    }

    /** Os convites por usar de uma liga, por equipa, para a tabela do gestor. */
    @Transactional(readOnly = true)
    public List<ConviteTreinador> pendentesDaLiga(UUID ligaId) {
        return conviteRepository.listarPendentesPorLiga(ligaId).stream()
                .filter(ConviteTreinador::estaDisponivel)
                .toList();
    }

    /**
     * Revoga um convite deste lugar. A autorização é da equipa, não de quem o
     * emitiu: uma liga pode ter mudado de gestor desde então, e o convite
     * continua a ser daquela equipa.
     */
    @Transactional
    public ConviteTreinador revogar(UUID conviteId, UUID equipaId) {
        ConviteTreinador convite = conviteRepository.buscarPorIdEEquipa(conviteId, equipaId)
                // 404 e não 403: um gestor não fica a saber que existe um convite
                // de outra equipa com este id.
                .orElseThrow(() -> new RecursoNaoEncontradoException("Convite não encontrado."));

        if (convite.estaUsado()) {
            throw new IllegalStateException("Este convite já foi usado e não pode ser revogado.");
        }
        if (convite.estaRevogado()) {
            throw new IllegalStateException("Este convite já estava revogado.");
        }

        convite.revogar();
        return conviteRepository.guardar(convite);
    }

    /**
     * Revoga tudo o que ainda esteja por usar para este treinador. Chamado
     * quando o lugar ganha conta: sem isto, um segundo convite emitido antes
     * ficava válido para sempre — a apontar a um lugar já ocupado, e a rebentar
     * com um conflito no dia em que alguém o usasse.
     */
    @Transactional
    public void revogarPendentes(Treinador treinador) {
        conviteRepository.listarPendentesPorTreinador(treinador.getId()).forEach(convite -> {
            convite.revogar();
            conviteRepository.guardar(convite);
        });
    }

    /**
     * Devolve o convite se ele servir para criar uma conta agora.
     *
     * <p>Separado do {@link #consumir}: quem se regista precisa de saber a que
     * lugar o convite pertence <em>antes</em> de a conta poder ser criada, e
     * só depois é que o convite pode ser marcado como usado.
     */
    @Transactional(readOnly = true)
    public ConviteTreinador exigirDisponivel(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new ConviteInvalidoException("Código de convite inválido.");
        }

        ConviteTreinador convite = conviteRepository.buscarPorCodigo(codigo.trim())
                // mensagem igual à de um convite gasto: não distingue "não existe" de "já usado"
                .orElseThrow(() -> new ConviteInvalidoException("Código de convite inválido."));

        if (!convite.estaDisponivel()) {
            throw new ConviteInvalidoException("Código de convite inválido.");
        }
        return convite;
    }

    /**
     * Gasta o convite. Chamado dentro da transação que liga o treinador à
     * conta: se essa ligação falhar, o convite não fica gasto.
     */
    @Transactional
    public ConviteTreinador consumir(ConviteTreinador convite, Gestor conta) {
        if (!convite.estaDisponivel()) {
            throw new ConviteInvalidoException("Código de convite inválido.");
        }
        convite.marcarUsado(conta);
        return conviteRepository.guardar(convite);
    }

    private Optional<ConviteTreinador> disponivelDoTreinador(UUID treinadorId) {
        return conviteRepository.listarPendentesPorTreinador(treinadorId).stream()
                // Pendente na base de dados não é o mesmo que utilizável: a
                // expiração compara-se com o relógio, não com uma coluna.
                .filter(ConviteTreinador::estaDisponivel)
                .findFirst();
    }
}
