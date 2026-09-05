package com.ligarecord.service;

import com.ligarecord.domain.ConviteTreinador;
import com.ligarecord.domain.Gestor;
import com.ligarecord.domain.Treinador;
import com.ligarecord.repository.GestorRepository;
import com.ligarecord.repository.TreinadorRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Liga um {@link Treinador} a uma conta a partir de um convite. A conta vive
 * na mesma tabela que a do gestor ({@link Gestor}): quem já é gestor de outra
 * liga, ou já treina outra equipa, usa {@link #ligar} com a conta que já tem
 * em vez de {@link #registar}, que cria uma conta nova.
 *
 * <p>Um treinador sem email, ou que não queira conta nenhuma, simplesmente
 * nunca passa por aqui: o convite fica por aceitar e o gestor continua a
 * gerir a equipa e as dívidas por ele.
 */
@Service
public class TreinadorContaService {

    private final GestorRepository gestorRepository;
    private final TreinadorRepository treinadorRepository;
    private final ConviteTreinadorService conviteService;
    private final PasswordEncoder passwordEncoder;

    public TreinadorContaService(GestorRepository gestorRepository,
                                 TreinadorRepository treinadorRepository,
                                 ConviteTreinadorService conviteService,
                                 PasswordEncoder passwordEncoder) {
        this.gestorRepository = gestorRepository;
        this.treinadorRepository = treinadorRepository;
        this.conviteService = conviteService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Cria uma conta nova a partir de um convite válido e liga-a ao treinador
     * do convite. Para quem ainda não tem conta nenhuma na aplicação.
     *
     * <p>Tudo corre na mesma transação: se a ligação ao treinador falhar
     * depois do convite ser marcado, nada é gravado e o convite continua
     * utilizável.
     */
    @Transactional
    public Gestor registar(String email, String password, String nome, String codigo) {
        String emailNormalizado = RegrasDeConta.emailNormalizado(email);
        String nomeValidado = RegrasDeConta.nomeValidado(nome);
        RegrasDeConta.validarPassword(password);

        if (gestorRepository.buscarPorEmail(emailNormalizado).isPresent()) {
            throw new IllegalStateException("Já existe uma conta com este email.");
        }

        ConviteTreinador convite = conviteService.exigirDisponivel(codigo);

        Gestor conta = new Gestor(
                UUID.randomUUID(),
                emailNormalizado,
                passwordEncoder.encode(password),
                nomeValidado
        );
        // Só foi convidado para treinar uma equipa — não fica, de brinde, a
        // poder criar ligas próprias. Só um administrador concede isso.
        conta.setPodeCriarLigas(false);
        conta = gestorRepository.guardar(conta);

        ligarTreinador(convite, conta);
        return conta;
    }

    /**
     * Liga o treinador do convite a uma conta que já existe — quem já é
     * gestor de outra liga, ou já treina outra equipa, e não precisa de um
     * segundo login.
     */
    @Transactional
    public void ligar(String codigo, Gestor contaExistente) {
        if (contaExistente == null) {
            throw new IllegalArgumentException("É preciso indicar a conta a ligar.");
        }
        ConviteTreinador convite = conviteService.exigirDisponivel(codigo);
        ligarTreinador(convite, contaExistente);
    }

    private void ligarTreinador(ConviteTreinador convite, Gestor conta) {
        Treinador treinador = convite.getTreinador();

        // O convite pode ter sido emitido antes de o treinador ganhar conta por
        // outra via (ex.: um segundo convite ainda válido para o mesmo
        // treinador); sem esta verificação, ligar substituía a conta já ligada
        // em silêncio, sem erro nenhum.
        if (treinador.temConta()) {
            throw new IllegalStateException("Este treinador já tem conta.");
        }

        treinador.setConta(conta);
        treinadorRepository.guardar(treinador);
        conviteService.consumir(convite, conta);
    }
}
