package com.ligarecord.web;

import com.ligarecord.domain.Equipa;
import com.ligarecord.domain.Gestor;
import com.ligarecord.domain.Liga;
import com.ligarecord.domain.Treinador;
import com.ligarecord.domain.enums.EstadoEquipa;
import com.ligarecord.repository.EquipaRepository;
import com.ligarecord.repository.GestorRepository;
import com.ligarecord.repository.LigaLogoRepository;
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
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/ligas")
public class LigaController {

    /** O logo de uma liga tem de ser uma imagem, e não uma imagem qualquer:
     *  SVG fica de fora de propósito — servido do nosso domínio, um SVG do
     *  utilizador podia trazer scripts (XSS). */
    private static final Set<String> TIPOS_LOGO = Set.of("image/png", "image/jpeg", "image/webp");
    private static final long LOGO_MAX_BYTES = 1_000_000L;

    private final LigaService ligaService;
    private final ClassificacaoService classificacaoService;
    private final LigaRepository ligaRepository;
    private final LigaLogoRepository ligaLogoRepository;
    private final EquipaRepository equipaRepository;
    private final GestorRepository gestorRepository;

    public LigaController(LigaService ligaService,
                          ClassificacaoService classificacaoService,
                          LigaRepository ligaRepository,
                          LigaLogoRepository ligaLogoRepository,
                          EquipaRepository equipaRepository,
                          GestorRepository gestorRepository) {
        this.ligaService = ligaService;
        this.classificacaoService = classificacaoService;
        this.ligaRepository = ligaRepository;
        this.ligaLogoRepository = ligaLogoRepository;
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

    /**
     * O logo é identidade da liga (do tenant), não do produto: por isso vive
     * aqui, atrás da autorização por dono, e não nos recursos públicos.
     */
    @PostMapping("/{ligaId}/logo")
    @Transactional
    public LigaDto carregarLogo(@AuthenticationPrincipal GestorAutenticado autenticado,
                                @PathVariable UUID ligaId,
                                @RequestParam("ficheiro") MultipartFile ficheiro) {
        Liga liga = liga(autenticado, ligaId);
        if (ficheiro == null || ficheiro.isEmpty()) {
            throw new IllegalArgumentException("Escolhe uma imagem.");
        }
        String tipo = ficheiro.getContentType();
        if (tipo == null || !TIPOS_LOGO.contains(tipo)) {
            throw new IllegalArgumentException("O logo tem de ser PNG, JPEG ou WEBP.");
        }
        if (ficheiro.getSize() > LOGO_MAX_BYTES) {
            throw new IllegalArgumentException("A imagem é demasiado grande (máx. 1 MB).");
        }
        byte[] dados;
        try {
            dados = ficheiro.getBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível ler a imagem.");
        }
        ligaLogoRepository.guardar(ligaId, dados);
        liga.setLogoTipo(tipo);
        ligaRepository.guardarLiga(liga);
        return LigaDto.de(liga);
    }

    @GetMapping("/{ligaId}/logo")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> logo(@AuthenticationPrincipal GestorAutenticado autenticado,
                                       @PathVariable UUID ligaId) {
        Liga liga = liga(autenticado, ligaId);
        byte[] dados = ligaLogoRepository.buscar(ligaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Esta liga não tem logo."));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(liga.getLogoTipo()))
                .cacheControl(CacheControl.noCache())
                .body(dados);
    }

    @DeleteMapping("/{ligaId}/logo")
    @Transactional
    public LigaDto removerLogo(@AuthenticationPrincipal GestorAutenticado autenticado,
                               @PathVariable UUID ligaId) {
        Liga liga = liga(autenticado, ligaId);
        ligaLogoRepository.apagar(ligaId);
        liga.setLogoTipo(null);
        ligaRepository.guardarLiga(liga);
        return LigaDto.de(liga);
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
