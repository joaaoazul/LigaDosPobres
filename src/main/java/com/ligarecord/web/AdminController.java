package com.ligarecord.web;

import com.ligarecord.domain.Gestor;
import com.ligarecord.domain.enums.PapelGestor;
import com.ligarecord.repository.GestorRepository;
import com.ligarecord.security.GestorAutenticado;
import com.ligarecord.service.AdminService;
import com.ligarecord.service.ConviteService;
import com.ligarecord.web.dto.AlterarGestorRequest;
import com.ligarecord.web.dto.ConviteDto;
import com.ligarecord.web.dto.CriarConviteRequest;
import com.ligarecord.web.dto.GestorAdminDto;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Administração de contas e convites. Todo este controller está atrás de
 * {@code hasRole("ADMIN")}, definido no SecurityConfig.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final ConviteService conviteService;
    private final GestorRepository gestorRepository;

    public AdminController(AdminService adminService,
                           ConviteService conviteService,
                           GestorRepository gestorRepository) {
        this.adminService = adminService;
        this.conviteService = conviteService;
        this.gestorRepository = gestorRepository;
    }

    @GetMapping("/gestores")
    @Transactional(readOnly = true)
    public List<GestorAdminDto> listarGestores() {
        return adminService.listarGestores().stream().map(GestorAdminDto::de).toList();
    }

    @PatchMapping("/gestores/{gestorId}")
    public GestorAdminDto alterarGestor(@AuthenticationPrincipal GestorAutenticado admin,
                                        @PathVariable UUID gestorId,
                                        @RequestBody AlterarGestorRequest pedido) {

        if (pedido.ativo() == null && pedido.papel() == null) {
            throw new IllegalArgumentException("Indica o estado ou o papel a alterar.");
        }

        Gestor gestor = null;

        if (pedido.ativo() != null) {
            gestor = adminService.alterarEstado(admin.getId(), gestorId, pedido.ativo());
        }
        if (pedido.papel() != null) {
            gestor = adminService.alterarPapel(admin.getId(), gestorId, papel(pedido.papel()));
        }

        return GestorAdminDto.de(gestor);
    }

    @GetMapping("/convites")
    @Transactional(readOnly = true)
    public List<ConviteDto> listarConvites() {
        return conviteService.listar().stream().map(ConviteDto::de).toList();
    }

    @PostMapping("/convites")
    @Transactional
    public ResponseEntity<ConviteDto> criarConvite(@AuthenticationPrincipal GestorAutenticado admin,
                                                   @RequestBody CriarConviteRequest pedido) {
        Gestor gestor = gestorRepository.buscarPorId(admin.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Gestor não encontrado."));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ConviteDto.de(conviteService.criar(gestor, pedido.nota(), pedido.diasValidade())));
    }

    @DeleteMapping("/convites/{conviteId}")
    public ConviteDto revogarConvite(@PathVariable UUID conviteId) {
        return ConviteDto.de(conviteService.revogar(conviteId));
    }

    private PapelGestor papel(String valor) {
        try {
            return PapelGestor.valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Papel inválido: usa GESTOR ou ADMIN.");
        }
    }
}
