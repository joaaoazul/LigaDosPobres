package com.ligarecord.service;

import com.ligarecord.domain.ContaTreinador;
import com.ligarecord.domain.ConviteTreinador;
import com.ligarecord.domain.Gestor;
import com.ligarecord.domain.Treinador;
import com.ligarecord.repository.ContaTreinadorRepository;
import com.ligarecord.repository.ConviteTreinadorRepository;
import com.ligarecord.web.ConviteInvalidoException;
import com.ligarecord.web.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Convites que dão conta a um treinador que já existe no domínio.
 *
 * <p>Distinto do {@link ConviteService}, que serve para criar contas de gestor:
 * ali o convite é aberto — quem o tiver escolhe quem é — e só um administrador o
 * cria; aqui o convite já nasce apontado a um treinador concreto, e é o gestor
 * dono da liga que o emite. Partilhar uma só classe obrigaria a campos opcionais
 * que metade dos convites nunca usaria, e a validações que só se aplicam a
 * alguns; separados, cada um diz exactamente o que é.
 */
@Service
public class ConviteTreinadorService {

    private static final int VALIDADE_MAXIMA_DIAS = 365;

    private final ConviteTreinadorRepository conviteRepository;
    private final ContaTreinadorRepository contaRepository;

    public ConviteTreinadorService(ConviteTreinadorRepository conviteRepository,
                                   ContaTreinadorRepository contaRepository) {
        this.conviteRepository = conviteRepository;
        this.contaRepository = contaRepository;
    }

    /**
     * Emite um convite para o treinador indicado.
     *
     * <p>Quem chama tem de ter resolvido a equipa do treinador por
     * {@code buscarPorIdEGestor} — é essa consulta que prova que o gestor manda
     * na liga onde o treinador tem equipa. Aqui verifica-se apenas o que essa
     * consulta não pode saber: que o treinador ainda não tem conta.
     */
    @Transactional
    public ConviteTreinador criar(Gestor criadoPor, Treinador treinador, Integer diasValidade) {
        if (criadoPor == null) {
            throw new IllegalArgumentException("O convite tem de ter um autor.");
        }
        if (treinador == null) {
            throw new IllegalArgumentException("O convite tem de ter um treinador.");
        }
        if (diasValidade != null && (diasValidade < 1 || diasValidade > VALIDADE_MAXIMA_DIAS)) {
            throw new IllegalArgumentException(
                    "A validade tem de estar entre 1 e " + VALIDADE_MAXIMA_DIAS + " dias.");
        }
        if (contaRepository.existePorTreinador(treinador.getId())) {
            throw new IllegalStateException("Este treinador já tem conta.");
        }

        Instant expiraEm = diasValidade == null
                ? null
                : Instant.now().plus(diasValidade, ChronoUnit.DAYS);

        return conviteRepository.guardar(new ConviteTreinador(
                UUID.randomUUID(),
                CodigosDeConvite.gerar(),
                treinador,
                criadoPor,
                expiraEm
        ));
    }

    @Transactional(readOnly = true)
    public List<ConviteTreinador> listar(UUID gestorId) {
        return conviteRepository.listarPorGestor(gestorId);
    }

    @Transactional
    public ConviteTreinador revogar(UUID conviteId, UUID gestorId) {
        ConviteTreinador convite = conviteRepository.buscarPorIdEGestor(conviteId, gestorId)
                // 404 e não 403: um gestor não fica a saber que existe um convite
                // de outro gestor com este id.
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
     * Devolve o convite se ele servir para criar uma conta agora.
     *
     * <p>Separado do {@link #consumir}: quem se regista precisa de saber a que
     * treinador o convite pertence <em>antes</em> de a conta poder ser criada, e
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
     * Gasta o convite. Chamado dentro da transação que cria a conta: se a
     * criação falhar, o convite não fica gasto.
     */
    @Transactional
    public ConviteTreinador consumir(ConviteTreinador convite, ContaTreinador conta) {
        if (!convite.estaDisponivel()) {
            throw new ConviteInvalidoException("Código de convite inválido.");
        }
        convite.marcarUsado(conta);
        return conviteRepository.guardar(convite);
    }
}
