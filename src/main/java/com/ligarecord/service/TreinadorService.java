package com.ligarecord.service;

import com.ligarecord.domain.Treinador;
import com.ligarecord.repository.TreinadorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * O lugar de treinador de uma equipa: criá-lo, corrigi-lo e desligar dele a
 * conta de quem saiu.
 *
 * <p>Existe para o nome e o email passarem pelas mesmas regras venham de onde
 * vierem — do formulário que inscreve a equipa ou da correcção feita depois.
 * Enquanto o {@code LigaController} construía o {@link Treinador} com as
 * próprias mãos, o email não tinha por onde ser validado: as
 * {@link RegrasDeConta} não são visíveis fora deste pacote.
 */
@Service
public class TreinadorService {

    private final TreinadorRepository treinadorRepository;

    public TreinadorService(TreinadorRepository treinadorRepository) {
        this.treinadorRepository = treinadorRepository;
    }

    /**
     * O lugar de uma equipa que está a ser inscrita. Não é guardado aqui: vai
     * agarrado à equipa, que o {@code LigaService} persiste em cascata.
     */
    public Treinador novo(String nome, String email) {
        Treinador treinador = new Treinador(UUID.randomUUID(), RegrasDeConta.nomeValidado(nome));
        treinador.setEmail(emailOpcional(email));
        return treinador;
    }

    /**
     * Corrige o nome e o email do lugar. O email a vazio apaga o que lá
     * estivesse — é como se tira um contacto que deixou de servir.
     */
    @Transactional
    public Treinador alterar(Treinador treinador, String nome, String email) {
        treinador.setNome(RegrasDeConta.nomeValidado(nome));
        treinador.setEmail(emailOpcional(email));
        return treinadorRepository.guardar(treinador);
    }

    /**
     * Desliga a conta do lugar — o treinador saiu, e quem entrar a seguir não
     * herda o acesso de quem lá estava. A dívida não vai atrás: é da equipa.
     */
    @Transactional
    public Treinador desligarConta(Treinador treinador) {
        if (!treinador.temConta()) {
            throw new IllegalStateException("Este lugar não tem conta ligada.");
        }
        treinador.setConta(null);
        return treinadorRepository.guardar(treinador);
    }

    /**
     * Nem todo o treinador tem email, e isso é um estado normal — não um campo
     * por preencher. Quando vem, passa pelas mesmas regras do email de uma
     * conta: em minúsculas, e recusado se for obviamente inválido.
     */
    private String emailOpcional(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return RegrasDeConta.emailNormalizado(email);
    }
}
