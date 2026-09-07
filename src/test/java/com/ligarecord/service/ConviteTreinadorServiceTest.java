package com.ligarecord.service;

import com.ligarecord.domain.ConviteTreinador;

import com.ligarecord.domain.Equipa;
import com.ligarecord.domain.Gestor;
import com.ligarecord.domain.Liga;
import com.ligarecord.domain.Treinador;
import com.ligarecord.domain.enums.EstadoEquipa;
import com.ligarecord.domain.enums.EstadoLiga;
import com.ligarecord.email.EnviadorParaLog;
import com.ligarecord.repository.ConviteTreinadorRepository;
import com.ligarecord.repository.ConviteTreinadorRepositoryImpl;
import com.ligarecord.web.ConviteInvalidoException;
import com.ligarecord.web.RecursoNaoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConviteTreinadorServiceTest {

    private ConviteTreinadorRepository conviteRepository;
    private ConviteTreinadorService conviteService;
    private Gestor gestor;
    private Gestor outroGestor;
    private Treinador treinador;
    private Equipa equipa;
    private Equipa outraEquipa;

    @BeforeEach
    void setUp() {
        conviteRepository = new ConviteTreinadorRepositoryImpl();
        conviteService = new ConviteTreinadorService(conviteRepository,
                new EnviadorParaLog(), new LinksDaAplicacao("https://liga.exemplo.pt"));

        gestor = new Gestor(UUID.randomUUID(), "gestor@teste.pt", "hash", "Gestor");
        outroGestor = new Gestor(UUID.randomUUID(), "outro@teste.pt", "hash", "Outro");
        treinador = new Treinador(UUID.randomUUID(), "João Azul");

        Liga liga = new Liga(UUID.randomUUID(), "Liga de Teste", 10, EstadoLiga.ATIVA, gestor);
        equipa = new Equipa(UUID.randomUUID(), "Leões", treinador, liga, EstadoEquipa.ATIVA);
        outraEquipa = new Equipa(UUID.randomUUID(), "Águias",
                new Treinador(UUID.randomUUID(), "Outro Treinador"), liga, EstadoEquipa.ATIVA);
    }

    private ConviteTreinador emitir() {
        return conviteService.emitir(gestor, equipa, null).convite();
    }

    @Test
    void deveEmitirConviteApontadoAoLugar() {
        ConviteTreinador convite = emitir();

        assertEquals(equipa, convite.getEquipa());
        assertEquals(treinador, convite.getTreinador());
        assertEquals(gestor, convite.getCriadoPor());
        assertTrue(convite.estaDisponivel());
    }

    @Test
    void codigosNaoSeRepetem() {
        assertNotEquals(
                emitir().getCodigo(),
                conviteService.emitir(gestor, outraEquipa, null).convite().getCodigo());
    }

    /**
     * Carregar duas vezes no botão espalhava duas credenciais válidas para a
     * mesma equipa, e a primeira ficava a valer sem ninguém saber onde parava.
     */
    @Test
    void emitirDuasVezesDevolveOMesmoConvite() {
        ConviteTreinadorService.Emissao primeira = conviteService.emitir(gestor, equipa, null);
        ConviteTreinadorService.Emissao segunda = conviteService.emitir(gestor, equipa, null);

        assertTrue(primeira.novo());
        assertFalse(segunda.novo());
        assertSame(primeira.convite(), segunda.convite());
    }

    /** Revogado o anterior, o botão volta a emitir — e emite um novo. */
    @Test
    void depoisDeRevogarVoltaAEmitirUmConviteNovo() {
        ConviteTreinador primeiro = emitir();
        conviteService.revogar(primeiro.getId(), equipa.getId());

        ConviteTreinadorService.Emissao segunda = conviteService.emitir(gestor, equipa, null);

        assertTrue(segunda.novo());
        assertNotEquals(primeiro.getCodigo(), segunda.convite().getCodigo());
    }

    /** Um convite é uma credencial: sem prazo andava em conversas para sempre. */
    @Test
    void conviteSemValidadePedidaExpiraNaValidadePorOmissao() {
        ConviteTreinador convite = emitir();

        assertNotNull(convite.getExpiraEm());
        Instant esperado = Instant.now().plus(ConviteTreinadorService.VALIDADE_OMISSAO_DIAS, ChronoUnit.DAYS);
        assertTrue(Math.abs(esperado.getEpochSecond() - convite.getExpiraEm().getEpochSecond()) < 60);
    }

    @Test
    void naoDeveConvidarTreinadorQueJaTemConta() {
        treinador.setConta(gestor);

        assertThrows(IllegalStateException.class, () -> conviteService.emitir(gestor, equipa, null));
    }

    @Test
    void naoDeveAceitarValidadeForaDoIntervalo() {
        assertThrows(IllegalArgumentException.class, () -> conviteService.emitir(gestor, equipa, 0));
        assertThrows(IllegalArgumentException.class, () -> conviteService.emitir(gestor, equipa, 400));
    }

    /**
     * A autorização é feita na consulta, e é pela equipa e não por quem emitiu:
     * o convite é do lugar, e uma liga pode ter mudado de gestor entretanto.
     */
    @Test
    void naoDeveRevogarConviteDeOutraEquipa() {
        ConviteTreinador convite = emitir();

        assertThrows(
                RecursoNaoEncontradoException.class,
                () -> conviteService.revogar(convite.getId(), outraEquipa.getId()));
    }

    /** Quem herdou a liga revoga os convites que o gestor anterior emitiu. */
    @Test
    void oGestorNovoDaLigaRevogaOConviteDoAnterior() {
        ConviteTreinador convite = conviteService.emitir(outroGestor, equipa, null).convite();

        assertTrue(conviteService.revogar(convite.getId(), equipa.getId()).estaRevogado());
    }

    @Test
    void conviteRevogadoDeixaDeServir() {
        ConviteTreinador convite = emitir();
        conviteService.revogar(convite.getId(), equipa.getId());

        assertThrows(
                ConviteInvalidoException.class,
                () -> conviteService.exigirDisponivel(convite.getCodigo()));
    }

    @Test
    void conviteUsadoDeixaDeServir() {
        ConviteTreinador convite = emitir();
        Gestor conta = new Gestor(UUID.randomUUID(), "joao@teste.pt", "hash", "João");

        conviteService.consumir(convite, conta);

        assertThrows(
                ConviteInvalidoException.class,
                () -> conviteService.exigirDisponivel(convite.getCodigo()));
    }

    @Test
    void conviteInexistenteEUsadoDaoAMesmaResposta() {
        ConviteTreinador convite = emitir();
        conviteService.consumir(convite, new Gestor(UUID.randomUUID(), "joao@teste.pt", "hash", "João"));

        ConviteInvalidoException usado = assertThrows(ConviteInvalidoException.class,
                () -> conviteService.exigirDisponivel(convite.getCodigo()));
        ConviteInvalidoException inexistente = assertThrows(ConviteInvalidoException.class,
                () -> conviteService.exigirDisponivel("nao-existe"));

        assertEquals(usado.getMessage(), inexistente.getMessage());
    }

    @Test
    void revogarPendentesInvalidaTudoOQueSobrouParaOLugar() {
        ConviteTreinador convite = emitir();

        conviteService.revogarPendentes(treinador);

        assertTrue(convite.estaRevogado());
        assertThrows(
                ConviteInvalidoException.class,
                () -> conviteService.exigirDisponivel(convite.getCodigo()));
    }

    /** A tabela de equipas do gestor pergunta por liga, não convite a convite. */
    @Test
    void pendentesDaLigaSoTrazemOsQueAindaServem() {
        ConviteTreinador daEquipa = emitir();
        ConviteTreinador daOutra = conviteService.emitir(gestor, outraEquipa, null).convite();
        conviteService.revogar(daOutra.getId(), outraEquipa.getId());

        Liga liga = equipa.getLiga();
        assertEquals(1, conviteService.pendentesDaLiga(liga.getId()).size());
        assertEquals(daEquipa, conviteService.pendentesDaLiga(liga.getId()).get(0));
    }
}
