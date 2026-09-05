package com.ligarecord.service;

import com.ligarecord.domain.ContaTreinador;
import com.ligarecord.domain.ConviteTreinador;
import com.ligarecord.domain.Gestor;
import com.ligarecord.domain.Treinador;
import com.ligarecord.repository.ContaTreinadorRepository;
import com.ligarecord.repository.ContaTreinadorRepositoryImpl;
import com.ligarecord.repository.ConviteTreinadorRepository;
import com.ligarecord.repository.ConviteTreinadorRepositoryImpl;
import com.ligarecord.web.ConviteInvalidoException;
import com.ligarecord.web.RecursoNaoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConviteTreinadorServiceTest {

    private ConviteTreinadorRepository conviteRepository;
    private ContaTreinadorRepository contaRepository;
    private ConviteTreinadorService conviteService;
    private Gestor gestor;
    private Gestor outroGestor;
    private Treinador treinador;

    @BeforeEach
    void setUp() {
        conviteRepository = new ConviteTreinadorRepositoryImpl();
        contaRepository = new ContaTreinadorRepositoryImpl();
        conviteService = new ConviteTreinadorService(conviteRepository, contaRepository);

        gestor = new Gestor(UUID.randomUUID(), "gestor@teste.pt", "hash", "Gestor");
        outroGestor = new Gestor(UUID.randomUUID(), "outro@teste.pt", "hash", "Outro");
        treinador = new Treinador(UUID.randomUUID(), "João Azul");
    }

    @Test
    void deveCriarConviteApontadoAoTreinador() {
        ConviteTreinador convite = conviteService.criar(gestor, treinador, null);

        assertEquals(treinador, convite.getTreinador());
        assertEquals(gestor, convite.getCriadoPor());
        assertTrue(convite.estaDisponivel());
    }

    @Test
    void codigosNaoSeRepetem() {
        assertNotEquals(
                conviteService.criar(gestor, treinador, null).getCodigo(),
                conviteService.criar(gestor, new Treinador(UUID.randomUUID(), "Outro"), null).getCodigo());
    }

    @Test
    void naoDeveConvidarTreinadorQueJaTemConta() {
        contaRepository.guardar(new ContaTreinador(
                UUID.randomUUID(), "joao@teste.pt", "hash", "João", treinador));

        assertThrows(IllegalStateException.class, () -> conviteService.criar(gestor, treinador, null));
    }

    @Test
    void naoDeveAceitarValidadeForaDoIntervalo() {
        assertThrows(IllegalArgumentException.class, () -> conviteService.criar(gestor, treinador, 0));
        assertThrows(IllegalArgumentException.class, () -> conviteService.criar(gestor, treinador, 400));
    }

    /** A autorização é feita na consulta: o convite de outro gestor não existe. */
    @Test
    void naoDeveRevogarConviteDeOutroGestor() {
        ConviteTreinador convite = conviteService.criar(gestor, treinador, null);

        assertThrows(
                RecursoNaoEncontradoException.class,
                () -> conviteService.revogar(convite.getId(), outroGestor.getId()));
    }

    @Test
    void conviteRevogadoDeixaDeServir() {
        ConviteTreinador convite = conviteService.criar(gestor, treinador, null);
        conviteService.revogar(convite.getId(), gestor.getId());

        assertThrows(
                ConviteInvalidoException.class,
                () -> conviteService.exigirDisponivel(convite.getCodigo()));
    }

    @Test
    void conviteUsadoDeixaDeServir() {
        ConviteTreinador convite = conviteService.criar(gestor, treinador, null);
        ContaTreinador conta = new ContaTreinador(
                UUID.randomUUID(), "joao@teste.pt", "hash", "João", treinador);

        conviteService.consumir(convite, conta);

        assertThrows(
                ConviteInvalidoException.class,
                () -> conviteService.exigirDisponivel(convite.getCodigo()));
    }

    @Test
    void conviteInexistenteEUsadoDaoAMesmaResposta() {
        ConviteTreinador convite = conviteService.criar(gestor, treinador, null);
        conviteService.consumir(convite, new ContaTreinador(
                UUID.randomUUID(), "joao@teste.pt", "hash", "João", treinador));

        ConviteInvalidoException usado = assertThrows(ConviteInvalidoException.class,
                () -> conviteService.exigirDisponivel(convite.getCodigo()));
        ConviteInvalidoException inexistente = assertThrows(ConviteInvalidoException.class,
                () -> conviteService.exigirDisponivel("nao-existe"));

        assertEquals(usado.getMessage(), inexistente.getMessage());
    }

    @Test
    void soListaOsConvitesDoProprioGestor() {
        conviteService.criar(gestor, treinador, null);
        conviteService.criar(outroGestor, new Treinador(UUID.randomUUID(), "Outro"), null);

        assertEquals(1, conviteService.listar(gestor.getId()).size());
    }
}
