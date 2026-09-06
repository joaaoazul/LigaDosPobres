package com.ligarecord.service;

import com.ligarecord.domain.Gestor;
import com.ligarecord.domain.enums.PapelGestor;
import com.ligarecord.repository.GestorRepository;
import com.ligarecord.web.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Operações de administração sobre contas.
 *
 * <p>As duas regras que aqui estão existem para a aplicação não poder ficar sem
 * ninguém que a administre: não é possível desativar ou despromover o último
 * administrador ativo, nem alterar a própria conta.
 */
@Service
public class AdminService {

    private final GestorRepository gestorRepository;

    public AdminService(GestorRepository gestorRepository) {
        this.gestorRepository = gestorRepository;
    }

    @Transactional(readOnly = true)
    public List<Gestor> listarGestores() {
        return gestorRepository.listarTodos();
    }

    @Transactional
    public Gestor alterarEstado(UUID adminId, UUID gestorId, boolean ativo) {
        Gestor gestor = buscar(gestorId);
        verificarNaoEProprio(adminId, gestorId, "estado");

        if (!ativo && gestor.isAdmin() && ultimoAdminAtivo(gestor)) {
            throw new IllegalStateException(
                    "Não é possível desativar o último administrador ativo.");
        }

        gestor.setAtivo(ativo);
        return gestorRepository.guardar(gestor);
    }

    @Transactional
    public Gestor alterarPapel(UUID adminId, UUID gestorId, PapelGestor papel) {
        Gestor gestor = buscar(gestorId);
        verificarNaoEProprio(adminId, gestorId, "papel");

        if (papel != PapelGestor.ADMIN && gestor.isAdmin() && ultimoAdminAtivo(gestor)) {
            throw new IllegalStateException(
                    "Não é possível despromover o último administrador ativo.");
        }

        gestor.setPapel(papel);
        return gestorRepository.guardar(gestor);
    }

    /**
     * Concede ou retira a uma conta a permissão de criar as suas próprias
     * ligas. Só interessa a quem se registou por convite de treinador — quem
     * é gestor pelo caminho normal já nasce com ela.
     */
    @Transactional
    public Gestor alterarPermissaoCriarLigas(UUID adminId, UUID gestorId, boolean podeCriarLigas) {
        Gestor gestor = buscar(gestorId);
        verificarNaoEProprio(adminId, gestorId, "acesso a criar ligas");

        gestor.setPodeCriarLigas(podeCriarLigas);
        return gestorRepository.guardar(gestor);
    }

    /**
     * Corrige o email de uma conta.
     *
     * <p>Existe por causa da recuperação de password: quem escreveu o email mal
     * no registo não recebe o link, e sem esta operação a conta ficava presa
     * para sempre, porque só o próprio pode mudar o email e o próprio já não
     * consegue entrar.
     *
     * <p>Corta as sessões abertas dessa conta. O email é metade das credenciais,
     * e quem o muda a outra pessoa está a mudar por onde ela entra: deixar
     * sessões vivas a essa conta seria deixar meia porta aberta.
     */
    @Transactional
    public Gestor alterarEmail(UUID adminId, UUID gestorId, String email) {
        Gestor gestor = buscar(gestorId);
        verificarNaoEProprio(adminId, gestorId, "email");

        String normalizado = RegrasDeConta.emailNormalizado(email);
        if (normalizado.equals(gestor.getEmail())) {
            throw new IllegalArgumentException("O email é o mesmo que já lá estava.");
        }
        if (gestorRepository.buscarPorEmail(normalizado).isPresent()) {
            throw new IllegalStateException("Já existe uma conta com este email.");
        }

        gestor.setEmail(normalizado);
        gestor.invalidarSessoesAbertas();
        return gestorRepository.guardar(gestor);
    }

    /**
     * Rede redundante. Quem administra não pode mexer na própria conta, logo há
     * sempre pelo menos dois administradores ativos no momento em que um age
     * sobre o outro, e esta condição não chega a verificar-se. Fica como defesa
     * caso a regra de não mexer na própria conta venha a ser relaxada.
     */
    private boolean ultimoAdminAtivo(Gestor gestor) {
        return gestor.isAtivo() && gestorRepository.contarAdminsAtivos() <= 1;
    }

    /**
     * Impedir a alteração da própria conta evita o engano mais provável, que é
     * um administrador tirar-se a si próprio o acesso a meio de uma limpeza.
     */
    private void verificarNaoEProprio(UUID adminId, UUID gestorId, String campo) {
        if (adminId.equals(gestorId)) {
            throw new IllegalStateException(
                    "Não podes alterar o " + campo + " da tua própria conta.");
        }
    }

    private Gestor buscar(UUID gestorId) {
        return gestorRepository.buscarPorId(gestorId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Gestor não encontrado."));
    }
}
