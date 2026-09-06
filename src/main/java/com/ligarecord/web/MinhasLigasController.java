package com.ligarecord.web;

import com.ligarecord.domain.Equipa;
import com.ligarecord.domain.Gestor;
import com.ligarecord.domain.Liga;
import com.ligarecord.repository.EquipaRepository;
import com.ligarecord.repository.GestorRepository;
import com.ligarecord.repository.TreinadorRepository;
import com.ligarecord.security.GestorAutenticado;
import com.ligarecord.service.ClassificacaoService;
import com.ligarecord.service.DividaService;
import com.ligarecord.web.dto.ClassificacaoDto;
import com.ligarecord.web.dto.LigaDetalheDto;
import com.ligarecord.web.dto.LigaDto;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * A vista de um treinador sobre as ligas em que treina alguma equipa: só
 * leitura, sem nenhuma das acções de gestão que {@link LigaController}
 * expõe. A autorização não é por dono da liga (o treinador não é o gestor),
 * mas por ter algum {@link Equipa} nessa liga através do seu {@code Treinador}.
 */
@RestController
@RequestMapping("/api/minhas-ligas")
public class MinhasLigasController {

    private final TreinadorRepository treinadorRepository;
    private final EquipaRepository equipaRepository;
    private final GestorRepository gestorRepository;
    private final ClassificacaoService classificacaoService;
    private final DividaService dividaService;

    public MinhasLigasController(TreinadorRepository treinadorRepository,
                                 EquipaRepository equipaRepository,
                                 GestorRepository gestorRepository,
                                 ClassificacaoService classificacaoService,
                                 DividaService dividaService) {
        this.treinadorRepository = treinadorRepository;
        this.equipaRepository = equipaRepository;
        this.gestorRepository = gestorRepository;
        this.classificacaoService = classificacaoService;
        this.dividaService = dividaService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<LigaDto> listar(@AuthenticationPrincipal GestorAutenticado autenticado) {
        Gestor conta = conta(autenticado);

        // Distintas por id: a mesma liga pode aparecer duas vezes se a conta
        // treinar mais do que uma equipa nela.
        return equipasTreinadas(conta).stream()
                .map(Equipa::getLiga)
                .collect(Collectors.toMap(Liga::getId, liga -> liga, (a, b) -> a, LinkedHashMap::new))
                .values().stream()
                .map(LigaDto::de)
                .toList();
    }

    @GetMapping("/{ligaId}")
    @Transactional(readOnly = true)
    public LigaDetalheDto detalhe(@AuthenticationPrincipal GestorAutenticado autenticado,
                                  @PathVariable UUID ligaId) {
        Gestor conta = conta(autenticado);

        Liga liga = equipasTreinadas(conta).stream()
                .map(Equipa::getLiga)
                .filter(l -> l.getId().equals(ligaId))
                .findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("Liga não encontrada."));

        List<ClassificacaoDto> classificacao = classificacaoService.calcularClassificacao(liga).stream()
                .map(ClassificacaoDto::de)
                .toList();
        // O pote é da liga toda, não das equipas desta conta: são valores
        // agregados, não dizem quanto cada equipa deve.
        return LigaDetalheDto.de(liga, classificacao, dividaService.calcularPoteDaLiga(liga));
    }

    private List<Equipa> equipasTreinadas(Gestor conta) {
        return treinadorRepository.buscarPorConta(conta).stream()
                .flatMap(treinador -> equipaRepository.buscarPorTreinador(treinador).stream())
                .toList();
    }

    private Gestor conta(GestorAutenticado autenticado) {
        return gestorRepository.buscarPorId(autenticado.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conta não encontrada."));
    }
}
