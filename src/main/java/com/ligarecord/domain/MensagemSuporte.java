package com.ligarecord.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Uma mensagem deixada no formulário de contacto da página de entrada.
 *
 * <p>Quem a escreve não tem sessão nenhuma — é para quem não consegue entrar
 * que o formulário existe — por isso não há aqui nenhuma ligação a uma conta:
 * o email é o que a pessoa escreveu, e pode não corresponder a conta nenhuma.
 *
 * <p>Fica guardada mesmo depois de o email sair. O envio é a comodidade (quem
 * responde lê na caixa de correio), não o único sítio onde a mensagem existe:
 * se o Resend falhar, ela continua aqui em vez de desaparecer entre o "enviar"
 * e o nada.
 *
 * <p>O {@code ip} serve só para travar o spam, contando pedidos da mesma
 * origem numa janela de tempo (ver {@code SuporteService}).
 */
@Entity
@Table(name = "mensagem_suporte")
public class MensagemSuporte extends EntidadeBase {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String assunto;

    @Column(nullable = false)
    private String mensagem;

    @Column
    private String ip;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    protected MensagemSuporte() {
        // exigido pelo Hibernate
    }

    public MensagemSuporte(UUID id, String nome, String email, String assunto,
                           String mensagem, String ip) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.assunto = assunto;
        this.mensagem = mensagem;
        this.ip = ip;
        this.criadoEm = Instant.now();
    }

    @Override
    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public String getAssunto() {
        return assunto;
    }

    public String getMensagem() {
        return mensagem;
    }

    public String getIp() {
        return ip;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
