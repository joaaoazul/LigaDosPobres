package com.ligarecord.service;

import com.ligarecord.domain.ContaTreinador;
import com.ligarecord.domain.ConviteTreinador;
import com.ligarecord.repository.ContaTreinadorRepository;
import com.ligarecord.repository.GestorRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ContaTreinadorService {

    private final ContaTreinadorRepository contaRepository;
    private final GestorRepository gestorRepository;
    private final ConviteTreinadorService conviteService;
    private final PasswordEncoder passwordEncoder;

    public ContaTreinadorService(ContaTreinadorRepository contaRepository,
                                 GestorRepository gestorRepository,
                                 ConviteTreinadorService conviteService,
                                 PasswordEncoder passwordEncoder) {
        this.contaRepository = contaRepository;
        this.gestorRepository = gestorRepository;
        this.conviteService = conviteService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Cria a conta do treinador a partir de um convite válido, que fica
     * consumido. O treinador a que a conta fica ligada vem do convite, nunca do
     * pedido: quem se regista não escolhe de quem é a equipa que vai passar a ver.
     *
     * <p>Tudo corre na mesma transação: se a criação falhar depois do convite
     * ser marcado, nada é gravado e o convite continua utilizável.
     */
    @Transactional
    public ContaTreinador registar(String email, String password, String nome, String codigo) {
        String emailNormalizado = RegrasDeConta.emailNormalizado(email);
        String nomeValidado = RegrasDeConta.nomeValidado(nome);
        RegrasDeConta.validarPassword(password);
        exigirEmailLivre(emailNormalizado);

        ConviteTreinador convite = conviteService.exigirDisponivel(codigo);

        // O convite pode ter sido emitido antes de o treinador ganhar conta por
        // outra via; sem esta verificação, a segunda conta só falharia lá em
        // baixo, na restrição de unicidade da base de dados.
        if (contaRepository.existePorTreinador(convite.getTreinador().getId())) {
            throw new IllegalStateException("Este treinador já tem conta.");
        }

        ContaTreinador conta = new ContaTreinador(
                UUID.randomUUID(),
                emailNormalizado,
                passwordEncoder.encode(password),
                nomeValidado,
                convite.getTreinador()
        );

        conviteService.consumir(convite, conta);

        return contaRepository.guardar(conta);
    }

    /**
     * Muda a password da própria conta. Exige a password actual mesmo com sessão
     * aberta, pela mesma razão que em {@link GestorService}: sem isso, quem
     * apanhasse o computador destrancado ficava dono da conta.
     */
    @Transactional
    public void alterarPassword(UUID contaId, String atual, String nova) {
        ContaTreinador conta = contaRepository.buscarPorId(contaId)
                .orElseThrow(() -> new IllegalArgumentException("A conta não existe."));

        if (atual == null || !passwordEncoder.matches(atual, conta.getPasswordHash())) {
            throw new IllegalArgumentException("A password actual não está correcta.");
        }
        RegrasDeConta.validarPasswordNova(nova);
        if (passwordEncoder.matches(nova, conta.getPasswordHash())) {
            throw new IllegalArgumentException("A password nova tem de ser diferente da actual.");
        }

        conta.setPasswordHash(passwordEncoder.encode(nova));
        contaRepository.guardar(conta);
    }

    /**
     * O email tem de ser único nas duas tabelas de contas, e não apenas na sua.
     *
     * <p>A autenticação procura primeiro em gestores e só depois em treinadores.
     * Se o mesmo email existisse dos dois lados, a segunda conta nunca chegaria
     * a entrar — sem erro nenhum, apenas uma password "errada" que está certa.
     */
    private void exigirEmailLivre(String email) {
        if (contaRepository.buscarPorEmail(email).isPresent()
                || gestorRepository.buscarPorEmail(email).isPresent()) {
            throw new IllegalStateException("Já existe uma conta com este email.");
        }
    }
}
