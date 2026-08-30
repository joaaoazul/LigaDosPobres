package com.ligarecord.web;

import com.ligarecord.domain.Equipa;
import com.ligarecord.domain.Gestor;
import com.ligarecord.domain.Liga;
import com.ligarecord.domain.Treinador;
import com.ligarecord.domain.enums.EstadoEquipa;
import com.ligarecord.repository.EquipaRepository;
import com.ligarecord.repository.GestorRepository;
import com.ligarecord.repository.LigaRepository;
import com.ligarecord.security.GestorAutenticado;
import com.ligarecord.service.ClassificacaoService;
import com.ligarecord.service.LigaService;
import com.ligarecord.web.dto.AdicionarEquipaRequest;
import com.ligarecord.web.dto.ClassificacaoDto;
import com.ligarecord.web.dto.CriarLigaRequest;
import com.ligarecord.web.dto.EquipaDto;
import com.ligarecord.web.dto.LigaDetalheDto;
import com.ligarecord.web.dto.LigaDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ligas")
public class LigaController {

    private final LigaService ligaService;
    private final ClassificacaoService classificacaoService;
    private final LigaRepository ligaRepository;
    private final EquipaRepository equipaRepository;
    private final GestorRepository gestorRepository;

    public LigaController(LigaService ligaService,
                          ClassificacaoService classificacaoService,
                          LigaRepository ligaRepository,
                          EquipaRepository equipaRepository,
                          GestorRepository gestorRepository) {
        this.ligaService = ligaService;
        this.classificacaoService = classificacaoService;
        this.ligaRepository = ligaRepository;
        this.equipaRepository = equipaRepository;
        this.gestorRepository = gestorRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<LigaDto> listar(@AuthenticationPrincipal GestorAutenticado autenticado) {
        return ligaRepository.listarLigas(gestor(autenticado)).stream().map(LigaDto::de).toList();
    }

    @PostMapping
    /*
     * A transação abre no controller, e não só no serviço, porque a liga é
     * carregada aqui: sem uma transação a envolver a busca e a alteração, as
     * coleções lazy da liga já estão desligadas da sessão quando o serviço lhes
     * toca (LazyInitializationException).
     */
    @Transactional
    public ResponseEntity<LigaDto> criar(@AuthenticationPrincipal GestorAutenticado autenticado,
                                         @RequestBody CriarLigaRequest pedido) {
        Liga liga = ligaService.criarLiga(gestor(autenticado), pedido.nome(), pedido.maxEquipas());
        return ResponseEntity.status(HttpStatus.CREATED).body(LigaDto.de(liga));
    }

    @GetMapping("/{ligaId}")
    @Transactional(readOnly = true)
    public LigaDetalheDto detalhe(@AuthenticationPrincipal GestorAutenticado autenticado,
                                  @PathVariable UUID ligaId) {
        Liga liga = liga(autenticado, ligaId);
        return LigaDetalheDto.de(liga, classificacao(liga));
    }

    @GetMapping("/{ligaId}/classificacao")
    @Transactional(readOnly = true)
    public List<ClassificacaoDto> classificacao(@AuthenticationPrincipal GestorAutenticado autenticado,
                                                @PathVariable UUID ligaId) {
        return classificacao(liga(autenticado, ligaId));
    }

    @PostMapping("/{ligaId}/terminar")
    @Transactional
    public LigaDto terminar(@AuthenticationPrincipal GestorAutenticado autenticado,
                            @PathVariable UUID ligaId) {
        return LigaDto.de(ligaService.terminarLiga(liga(autenticado, ligaId)));
    }

    @PostMapping("/{ligaId}/equipas")
    @Transactional
    public ResponseEntity<EquipaDto> adicionarEquipa(@AuthenticationPrincipal GestorAutenticado autenticado,
                                                     @PathVariable UUID ligaId,
                                                     @RequestBody AdicionarEquipaRequest pedido) {
        if (pedido.nome() == null || pedido.nome().isBlank()) {
            throw new IllegalArgumentException("O nome da equipa é obrigatório.");
        }
        if (pedido.treinador() == null || pedido.treinador().isBlank()) {
            throw new IllegalArgumentException("O nome do treinador é obrigatório.");
        }

        Treinador treinador = new Treinador(UUID.randomUUID(), pedido.treinador().trim());
        Equipa equipa = new Equipa(
                UUID.randomUUID(),
                pedido.nome().trim(),
                treinador,
                null,
                EstadoEquipa.ATIVA
        );

        Equipa guardada = ligaService.adicionarEquipa(liga(autenticado, ligaId), equipa);
        return ResponseEntity.status(HttpStatus.CREATED).body(EquipaDto.de(guardada));
    }

    @PostMapping("/{ligaId}/equipas/{equipaId}/desistencia")
    @Transactional
    public EquipaDto registarDesistencia(@AuthenticationPrincipal GestorAutenticado autenticado,
                                         @PathVariable UUID ligaId,
                                         @PathVariable UUID equipaId) {
        Liga liga = liga(autenticado, ligaId);
        Equipa equipa = equipaRepository.buscarPorIdEGestor(equipaId, autenticado.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Equipa não encontrada."));
        return EquipaDto.de(ligaService.registarDesistencia(liga, equipa));
    }

    private List<ClassificacaoDto> classificacao(Liga liga) {
        return classificacaoService.calcularClassificacao(liga).stream().map(ClassificacaoDto::de).toList();
    }

    private Liga liga(GestorAutenticado autenticado, UUID ligaId) {
        return ligaRepository.buscarPorIdEGestor(ligaId, autenticado.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Liga não encontrada."));
    }

    private Gestor gestor(GestorAutenticado autenticado) {
        return gestorRepository.buscarPorId(autenticado.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Gestor não encontrado."));
    }
}
