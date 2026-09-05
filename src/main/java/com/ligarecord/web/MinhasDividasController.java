package com.ligarecord.web;

import com.ligarecord.domain.Equipa;
import com.ligarecord.domain.Gestor;
import com.ligarecord.domain.Treinador;
import com.ligarecord.repository.EquipaRepository;
import com.ligarecord.repository.GestorRepository;
import com.ligarecord.repository.TreinadorRepository;
import com.ligarecord.security.GestorAutenticado;
import com.ligarecord.service.DividaService;
import com.ligarecord.web.dto.DividaDto;
import com.ligarecord.web.dto.MinhasDividasDto;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * A vista de um treinador sobre as suas próprias dívidas — por equipa e por
 * liga, em todas as equipas que a sua conta treina. Só leitura: quem marca um
 * pagamento é sempre o gestor, em {@link DividaController}.
 *
 * <p>A lista parte do {@link Treinador} (quem a conta treina), não da
 * {@link com.ligarecord.domain.Divida} (quem já tem alguma coisa cobrada): uma
 * equipa que a conta treina mas que ainda não teve nenhum bloco fechado tem de
 * aparecer na mesma, a dever zero — não desaparecer da lista.
 */
@RestController
@RequestMapping("/api/minhas-dividas")
public class MinhasDividasController {

    private final DividaService dividaService;
    private final TreinadorRepository treinadorRepository;
    private final EquipaRepository equipaRepository;
    private final GestorRepository gestorRepository;

    public MinhasDividasController(DividaService dividaService,
                                   TreinadorRepository treinadorRepository,
                                   EquipaRepository equipaRepository,
                                   GestorRepository gestorRepository) {
        this.dividaService = dividaService;
        this.treinadorRepository = treinadorRepository;
        this.equipaRepository = equipaRepository;
        this.gestorRepository = gestorRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public MinhasDividasDto listar(@AuthenticationPrincipal GestorAutenticado autenticado) {
        Gestor conta = gestorRepository.buscarPorId(autenticado.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conta não encontrada."));

        List<DividaDto> equipas = treinadorRepository.buscarPorConta(conta).stream()
                .flatMap(treinador -> equipaRepository.buscarPorTreinador(treinador).stream())
                .map(this::dividaDe)
                .toList();

        BigDecimal totalGeral = equipas.stream()
                .map(DividaDto::totalPendente)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new MinhasDividasDto(equipas, totalGeral);
    }

    private DividaDto dividaDe(Equipa equipa) {
        return dividaService.buscarPorEquipa(equipa)
                .map(divida -> DividaDto.de(divida, dividaService.calcularTotalDivida(divida)))
                .orElseGet(() -> DividaDto.vazia(equipa));
    }
}
