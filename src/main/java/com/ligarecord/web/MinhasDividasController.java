package com.ligarecord.web;

import com.ligarecord.domain.Gestor;
import com.ligarecord.repository.GestorRepository;
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
 */
@RestController
@RequestMapping("/api/minhas-dividas")
public class MinhasDividasController {

    private final DividaService dividaService;
    private final GestorRepository gestorRepository;

    public MinhasDividasController(DividaService dividaService, GestorRepository gestorRepository) {
        this.dividaService = dividaService;
        this.gestorRepository = gestorRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public MinhasDividasDto listar(@AuthenticationPrincipal GestorAutenticado autenticado) {
        Gestor conta = gestorRepository.buscarPorId(autenticado.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conta não encontrada."));

        List<DividaDto> equipas = dividaService.listarPorTreinador(conta).stream()
                .map(divida -> DividaDto.de(divida, dividaService.calcularTotalDivida(divida)))
                .toList();

        BigDecimal totalGeral = equipas.stream()
                .map(DividaDto::totalPendente)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new MinhasDividasDto(equipas, totalGeral);
    }
}
