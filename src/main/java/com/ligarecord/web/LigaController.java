package com.ligarecord.web;

import com.ligarecord.domain.Equipa;
import com.ligarecord.domain.Gestor;
import com.ligarecord.domain.Liga;
import com.ligarecord.domain.Treinador;
import com.ligarecord.domain.enums.EstadoEquipa;
import com.ligarecord.domain.enums.EstadoLiga;
import com.ligarecord.repository.EquipaRepository;
import com.ligarecord.repository.GestorRepository;
import com.ligarecord.repository.LigaLogoRepository;
import com.ligarecord.repository.LigaRepository;
import com.ligarecord.security.GestorAutenticado;
import com.ligarecord.domain.ConviteTreinador;
import com.ligarecord.service.ClassificacaoService;
import com.ligarecord.service.ConviteTreinadorService;
import com.ligarecord.service.DividaService;
import com.ligarecord.service.TreinadorService;
import com.ligarecord.service.LigaService;
import com.ligarecord.web.dto.AdicionarEquipaRequest;
import com.ligarecord.web.dto.AlterarTreinadorRequest;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ligas")
public class LigaController {

    /** O formato é decidido pelos bytes ({@link ImagemSuportada}), não pelo
     *  cabeçalho do cliente. SVG fica de fora de propósito — servido do nosso
     *  domínio, um SVG do utilizador podia trazer scripts (XSS). */
    private static final long LOGO_MAX_BYTES = 1_000_000L;

    /** O logo muda raramente e o endereço leva um ?v= que muda quando ele muda,
     *  por isso o browser pode guardá-lo em vez de o voltar a puxar a cada
     *  redesenho do detalhe da liga. */
    private static final Duration LOGO_CACHE = Duration.ofDays(7);

    private final LigaService ligaService;
    private final ClassificacaoService classificacaoService;
    private final DividaService dividaService;
    private final LigaRepository ligaRepository;
    private final LigaLogoRepository ligaLogoRepository;
    private final EquipaRepository equipaRepository;
    private final GestorRepository gestorRepository;
    private final TreinadorService treinadorService;
    private final ConviteTreinadorService conviteTreinadorService;

    public LigaController(LigaService ligaService,
                          ClassificacaoService classificacaoService,
                          DividaService dividaService,
                          LigaRepository ligaRepository,
                          LigaLogoRepository ligaLogoRepository,
                          EquipaRepository equipaRepository,
                          GestorRepository gestorRepository,
                          TreinadorService treinadorService,
                          ConviteTreinadorService conviteTreinadorService) {
        this.ligaService = ligaService;
        this.classificacaoService = classificacaoService;
        this.dividaService = dividaService;
        this.ligaRepository = ligaRepository;
        this.ligaLogoRepository = ligaLogoRepository;
        this.equipaRepository = equipaRepository;
        this.gestorRepository = gestorRepository;
        this.treinadorService = treinadorService;
        this.conviteTreinadorService = conviteTreinadorService;
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
        return LigaDetalheDto.deParaGestor(liga, classificacao(liga),
                dividaService.calcularPoteDaLiga(liga), convitesPendentes(ligaId));
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
        // O nome e o email do treinador são validados pelo serviço: as regras
        // de contacto são as mesmas venham do formulário de inscrição ou da
        // correcção feita depois.
        Treinador treinador = treinadorService.novo(pedido.treinador(), pedido.treinadorEmail());
        Equipa equipa = new Equipa(
                UUID.randomUUID(),
                pedido.nome().trim(),
                treinador,
                null,
                EstadoEquipa.ATIVA
        );

        Equipa guardada = ligaService.adicionarEquipa(liga(autenticado, ligaId), equipa);
        // deParaGestor e não de: é o gestor que está do outro lado, e a resposta
        // tem de lhe mostrar o que ficou guardado — o email incluído. Uma equipa
        // acabada de inscrever nunca tem convite.
        return ResponseEntity.status(HttpStatus.CREATED).body(EquipaDto.deParaGestor(guardada, null));
    }

    @PostMapping("/{ligaId}/equipas/{equipaId}/desistencia")
    @Transactional
    public EquipaDto registarDesistencia(@AuthenticationPrincipal GestorAutenticado autenticado,
                                         @PathVariable UUID ligaId,
                                         @PathVariable UUID equipaId) {
        Liga liga = liga(autenticado, ligaId);
        Equipa equipa = equipaRepository.buscarPorIdEGestor(equipaId, autenticado.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Equipa não encontrada."));
        return EquipaDto.deParaGestor(ligaService.registarDesistencia(liga, equipa),
                pendenteDaEquipa(ligaId, equipaId));
    }

    /**
     * Muda o nome do lugar de treinador desta equipa — o rótulo que o gestor
     * lhe deu. Não mexe na conta ligada nem no nome de quem a tem: uma coisa é
     * como a liga trata o treinador, outra é o nome da pessoa.
     */
    @PatchMapping("/{ligaId}/equipas/{equipaId}/treinador")
    @Transactional
    public EquipaDto alterarTreinador(@AuthenticationPrincipal GestorAutenticado autenticado,
                                      @PathVariable UUID ligaId,
                                      @PathVariable UUID equipaId,
                                      @RequestBody AlterarTreinadorRequest pedido) {
        Equipa equipa = equipa(autenticado, ligaId, equipaId);
        treinadorService.alterar(equipa.getTreinador(), pedido.nome(), pedido.email());
        return EquipaDto.deParaGestor(equipa, pendenteDaEquipa(ligaId, equipaId));
    }

    /**
     * Desliga a conta do lugar: o treinador saiu, e quem entrar a seguir não
     * herda o acesso de quem lá estava.
     *
     * <p>A dívida não vai atrás — pertence à equipa ({@code divida.equipa_id}),
     * não a quem a treina — e é isso que torna a troca de treinador uma
     * operação inofensiva do ponto de vista do dinheiro.
     */
    @DeleteMapping("/{ligaId}/equipas/{equipaId}/treinador/conta")
    @Transactional
    public EquipaDto desligarContaDoTreinador(@AuthenticationPrincipal GestorAutenticado autenticado,
                                              @PathVariable UUID ligaId,
                                              @PathVariable UUID equipaId) {
        Equipa equipa = equipa(autenticado, ligaId, equipaId);
        treinadorService.desligarConta(equipa.getTreinador());
        return EquipaDto.deParaGestor(equipa, null);
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
        Liga liga = exigirAtiva(liga(autenticado, ligaId));
        if (ficheiro == null || ficheiro.isEmpty()) {
            throw new IllegalArgumentException("Escolhe uma imagem.");
        }
        if (ficheiro.getSize() > LOGO_MAX_BYTES) {
            throw new IllegalArgumentException("A imagem é demasiado grande (máx. 1 MB).");
        }

        byte[] dados;
        try {
            dados = ficheiro.getBytes();
        } catch (IOException e) {
            // Ler o ficheiro temporário do multipart falhou: disco cheio ou sem
            // permissões. É avaria do servidor, não do pedido — tem de chegar ao
            // log e devolver 500, senão o gestor repete para sempre um pedido
            // que nunca vai funcionar e o log não regista a avaria.
            throw new UncheckedIOException("Falha a ler o ficheiro do logo da liga " + ligaId, e);
        }

        String tipo = ImagemSuportada.tipoDe(dados);
        if (tipo == null) {
            throw new IllegalArgumentException("O logo tem de ser PNG, JPEG ou WEBP.");
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
                .cacheControl(CacheControl.maxAge(LOGO_CACHE).cachePrivate())
                .body(dados);
    }

    @DeleteMapping("/{ligaId}/logo")
    @Transactional
    public LigaDto removerLogo(@AuthenticationPrincipal GestorAutenticado autenticado,
                               @PathVariable UUID ligaId) {
        Liga liga = exigirAtiva(liga(autenticado, ligaId));
        ligaLogoRepository.apagar(ligaId);
        liga.setLogoTipo(null);
        ligaRepository.guardarLiga(liga);
        return LigaDto.de(liga);
    }

    private List<ClassificacaoDto> classificacao(Liga liga) {
        return classificacaoService.calcularClassificacao(liga).stream().map(ClassificacaoDto::de).toList();
    }

    /**
     * Uma liga desativada é só de leitura — é a regra que o LigaService impõe a
     * todas as outras alterações (equipas, desistências, jornadas). O logo não
     * é excepção: sem isto, era a única coisa ainda editável numa liga fechada.
     */
    private Liga exigirAtiva(Liga liga) {
        if (liga.getEstado() != EstadoLiga.ATIVA) {
            throw new IllegalStateException("Não é possível alterar o logo de uma liga desativada.");
        }
        return liga;
    }

    /** Os convites por usar da liga, indexados pela equipa a que pertencem. */
    private Map<UUID, ConviteTreinador> convitesPendentes(UUID ligaId) {
        return conviteTreinadorService.pendentesDaLiga(ligaId).stream()
                .collect(Collectors.toMap(
                        convite -> convite.getEquipa().getId(),
                        Function.identity(),
                        // Só pode haver um por equipa; se a base de dados
                        // trouxer dois (convites anteriores à V9), fica o mais
                        // recente, que é o que a consulta traz primeiro.
                        (primeiro, segundo) -> primeiro));
    }

    private ConviteTreinador pendenteDaEquipa(UUID ligaId, UUID equipaId) {
        return convitesPendentes(ligaId).get(equipaId);
    }

    private Equipa equipa(GestorAutenticado autenticado, UUID ligaId, UUID equipaId) {
        Equipa equipa = equipaRepository.buscarPorIdEGestor(equipaId, autenticado.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Equipa não encontrada."));
        if (equipa.getLiga() == null || !equipa.getLiga().getId().equals(ligaId)) {
            throw new RecursoNaoEncontradoException("Equipa não encontrada.");
        }
        return equipa;
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
