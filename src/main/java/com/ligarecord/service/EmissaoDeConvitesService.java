package com.ligarecord.service;

import com.ligarecord.domain.ConviteTreinador;
import com.ligarecord.domain.Equipa;
import com.ligarecord.domain.Gestor;
import com.ligarecord.domain.Liga;
import com.ligarecord.domain.Treinador;
import com.ligarecord.domain.enums.EstadoEquipa;
import com.ligarecord.repository.EquipaRepository;
import com.ligarecord.repository.GestorRepository;
import com.ligarecord.repository.LigaRepository;
import com.ligarecord.web.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Emitir convites de treinador com a autorização feita — uma equipa de cada
 * vez, ou a liga toda.
 *
 * <p>Só emite. <b>Os emails não saem daqui</b>, e é o ponto todo desta classe
 * existir separada: se saíssem dentro desta transação, uma falha a meio
 * desfazia convites já enviados e deixava links a apontar para nada — a única
 * coisa que não se pode retirar depois de sair. Quem chama emite primeiro —
 * isto grava e devolve — e só depois manda enviar, cada envio na sua transação.
 *
 * <p>Devolve registos e não entidades, pela mesma razão: depois do commit, as
 * entidades desta transação já não servem do outro lado.
 */
@Service
public class EmissaoDeConvitesService {

    private final LigaRepository ligaRepository;
    private final EquipaRepository equipaRepository;
    private final GestorRepository gestorRepository;
    private final ConviteTreinadorService conviteService;

    public EmissaoDeConvitesService(LigaRepository ligaRepository,
                                    EquipaRepository equipaRepository,
                                    GestorRepository gestorRepository,
                                    ConviteTreinadorService conviteService) {
        this.ligaRepository = ligaRepository;
        this.equipaRepository = equipaRepository;
        this.gestorRepository = gestorRepository;
        this.conviteService = conviteService;
    }

    /**
     * Um convite emitido, com o que é preciso para o entregar e para o
     * descrever a quem o pediu — tudo lido enquanto a transação ainda estava
     * aberta.
     */
    public record PorEntregar(UUID equipaId, String equipa, String treinador, String email,
                              UUID conviteId, String codigo, Instant criadoEm, Instant expiraEm,
                              boolean novo) {
    }

    /** Uma equipa que não precisou de convite, e porquê. */
    public record Fora(UUID equipaId, String equipa, String treinador, String motivo) {
    }

    public record Emissao(List<PorEntregar> porEntregar, List<Fora> fora) {
    }

    /**
     * Emite (ou reaproveita) o convite de uma equipa. A autorização acontece na
     * consulta: a equipa de outro gestor não existe.
     */
    @Transactional
    public PorEntregar emitirParaEquipa(UUID gestorId, UUID ligaId, UUID equipaId, Integer diasValidade) {
        Gestor gestor = gestorRepository.buscarPorId(gestorId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Gestor não encontrado."));
        Equipa equipa = equipaRepository.buscarPorIdEGestor(equipaId, gestorId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Equipa não encontrada."));
        if (equipa.getLiga() == null || !equipa.getLiga().getId().equals(ligaId)) {
            throw new RecursoNaoEncontradoException("Equipa não encontrada.");
        }
        return porEntregar(equipa, conviteService.emitir(gestor, equipa, diasValidade));
    }

    /**
     * Emite (ou reaproveita) o convite de cada equipa activa cujo treinador
     * ainda não tenha conta.
     *
     * <p>Reaproveitar é o que faz isto servir também de "lembra os que ainda
     * não aceitaram": quem já tinha convite por usar mantém o mesmo link, e o
     * envio a seguir é travado pelas mesmas regras de sempre.
     */
    @Transactional
    public Emissao emitirEmFalta(UUID gestorId, UUID ligaId) {
        Gestor gestor = gestorRepository.buscarPorId(gestorId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Gestor não encontrado."));
        Liga liga = ligaRepository.buscarPorIdEGestor(ligaId, gestorId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Liga não encontrada."));

        List<PorEntregar> porEntregar = new ArrayList<>();
        List<Fora> fora = new ArrayList<>();

        for (Equipa equipa : liga.getEquipas()) {
            Treinador treinador = equipa.getTreinador();

            if (treinador.temConta()) {
                fora.add(new Fora(equipa.getId(), equipa.getNome(), treinador.getNome(), "JA_TEM_CONTA"));
                continue;
            }
            // Uma equipa que desistiu não é convidada em lote. Se for mesmo
            // preciso, o botão da linha continua a convidá-la uma a uma.
            if (equipa.getEstado() != EstadoEquipa.ATIVA) {
                fora.add(new Fora(equipa.getId(), equipa.getNome(), treinador.getNome(), "DESISTENTE"));
                continue;
            }

            porEntregar.add(porEntregar(equipa, conviteService.emitir(gestor, equipa, null)));
        }

        return new Emissao(porEntregar, fora);
    }

    private PorEntregar porEntregar(Equipa equipa, ConviteTreinadorService.Emissao emissao) {
        ConviteTreinador convite = emissao.convite();
        return new PorEntregar(
                equipa.getId(),
                equipa.getNome(),
                equipa.getTreinador().getNome(),
                equipa.getTreinador().getEmail(),
                convite.getId(),
                convite.getCodigo(),
                convite.getCriadoEm(),
                convite.getExpiraEm(),
                emissao.novo());
    }
}
