package com.ligarecord.service;

import com.ligarecord.domain.Equipa;
import com.ligarecord.domain.Gestor;
import com.ligarecord.domain.Liga;
import com.ligarecord.domain.Treinador;
import com.ligarecord.domain.enums.EstadoEquipa;
import com.ligarecord.domain.enums.EstadoLiga;
import com.ligarecord.email.EnviadorParaLog;
import com.ligarecord.repository.ConviteTreinadorRepositoryImpl;
import com.ligarecord.repository.EquipaRepository;
import com.ligarecord.repository.EquipaRepositoryImpl;
import com.ligarecord.repository.GestorRepository;
import com.ligarecord.repository.GestorRepositoryImpl;
import com.ligarecord.repository.LigaRepository;
import com.ligarecord.repository.LigaRepositoryImpl;
import com.ligarecord.web.RecursoNaoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmissaoDeConvitesServiceTest {

    private LigaRepository ligaRepository;
    private GestorRepository gestorRepository;
    private ConviteTreinadorService conviteService;
    private EmissaoDeConvitesService emMassa;
    private Gestor gestor;
    private Liga liga;

    @BeforeEach
    void setUp() {
        ligaRepository = new LigaRepositoryImpl();
        gestorRepository = new GestorRepositoryImpl();
        conviteService = new ConviteTreinadorService(new ConviteTreinadorRepositoryImpl(),
                new EnviadorParaLog(), new LinksDaAplicacao("https://liga.exemplo.pt"));
        emMassa = new EmissaoDeConvitesService(ligaRepository, new EquipaRepositoryImpl(),
                gestorRepository, conviteService);

        gestor = new Gestor(UUID.randomUUID(), "gestor@teste.pt", "hash", "Gestor");
        gestorRepository.guardar(gestor);

        liga = new Liga(UUID.randomUUID(), "Liga do Café", 10, EstadoLiga.ATIVA, gestor);
        ligaRepository.guardarLiga(liga);
    }

    private Equipa equipa(String nome, String treinador) {
        Equipa equipa = new Equipa(UUID.randomUUID(), nome,
                new Treinador(UUID.randomUUID(), treinador), liga, EstadoEquipa.ATIVA);
        liga.getEquipas().add(equipa);
        return equipa;
    }

    @Test
    void convidaTodasAsEquipasSemConta() {
        equipa("Leões", "João");
        equipa("Águias", "Zé");

        EmissaoDeConvitesService.Emissao emissao = emMassa.emitirEmFalta(gestor.getId(), liga.getId());

        assertEquals(2, emissao.porEntregar().size());
        assertTrue(emissao.fora().isEmpty());
        // cada uma com o seu convite, e nenhum repetido
        assertEquals(2, emissao.porEntregar().stream().map(EmissaoDeConvitesService.PorEntregar::codigo)
                .distinct().count());
    }

    @Test
    void deixaDeForaQuemJaTemConta() {
        Equipa comConta = equipa("Leões", "João");
        comConta.getTreinador().setConta(gestor);
        equipa("Águias", "Zé");

        EmissaoDeConvitesService.Emissao emissao = emMassa.emitirEmFalta(gestor.getId(), liga.getId());

        assertEquals(1, emissao.porEntregar().size());
        assertEquals("Águias", emissao.porEntregar().get(0).equipa());
        assertEquals(List.of("JA_TEM_CONTA"),
                emissao.fora().stream().map(EmissaoDeConvitesService.Fora::motivo).toList());
    }

    /** Uma equipa que desistiu não é convidada em lote — mas o botão da linha continua a servir. */
    @Test
    void naoConvidaEquipasDesistentes() {
        Equipa desistente = equipa("Leões", "João");
        desistente.setEstado(EstadoEquipa.DESISTENTE);

        EmissaoDeConvitesService.Emissao emissao = emMassa.emitirEmFalta(gestor.getId(), liga.getId());

        assertTrue(emissao.porEntregar().isEmpty());
        assertEquals("DESISTENTE", emissao.fora().get(0).motivo());
    }

    /**
     * Correr duas vezes não espalha convites novos: é o que faz disto também um
     * "lembra os que ainda não aceitaram".
     */
    @Test
    void correrDuasVezesDaOsMesmosConvites() {
        equipa("Leões", "João");

        String primeiro = emMassa.emitirEmFalta(gestor.getId(), liga.getId())
                .porEntregar().get(0).codigo();
        String segundo = emMassa.emitirEmFalta(gestor.getId(), liga.getId())
                .porEntregar().get(0).codigo();

        assertEquals(primeiro, segundo);
    }

    @Test
    void aLigaDeOutroGestorNaoExiste() {
        Gestor outro = new Gestor(UUID.randomUUID(), "outro@teste.pt", "hash", "Outro");
        gestorRepository.guardar(outro);

        assertThrows(RecursoNaoEncontradoException.class,
                () -> emMassa.emitirEmFalta(outro.getId(), liga.getId()));
    }

    @Test
    void umaLigaSemEquipasNaoConvidaNinguem() {
        EmissaoDeConvitesService.Emissao emissao = emMassa.emitirEmFalta(gestor.getId(), liga.getId());

        assertTrue(emissao.porEntregar().isEmpty());
        assertTrue(emissao.fora().isEmpty());
    }
}
