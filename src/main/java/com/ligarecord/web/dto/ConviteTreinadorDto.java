package com.ligarecord.web.dto;

import com.ligarecord.domain.ConviteTreinador;
import com.ligarecord.service.ConviteTreinadorService;
import com.ligarecord.service.EmissaoDeConvitesService;

import java.time.Instant;
import java.util.UUID;

/**
 * O convite como o gestor o vê. O {@code link} é o que ele entrega ao
 * treinador — um endereço, e não um código de 32 caracteres para escrever à
 * mão — e, tal como o código, só existe enquanto o convite estiver por usar.
 */
public record ConviteTreinadorDto(
        UUID id,
        String codigo,
        String link,
        String treinadorNome,
        String equipaNome,
        String estado,
        Instant criadoEm,
        Instant expiraEm,
        Instant usadoEm,
        String envio,
        String enviadoPara) {

    public static ConviteTreinadorDto de(ConviteTreinador convite, String link) {
        return de(convite, link, null);
    }

    /**
     * {@code envio} diz o que aconteceu à tentativa de entregar o convite por
     * email — a null quando não se tentou, por o pedido não ser de emissão.
     */
    public static ConviteTreinadorDto de(ConviteTreinador convite, String link,
                                         ConviteTreinadorService.Envio envio) {
        boolean disponivel = convite.estaDisponivel();
        return new ConviteTreinadorDto(
                convite.getId(),
                // O código só é útil enquanto o convite estiver por usar.
                disponivel ? convite.getCodigo() : null,
                disponivel ? link : null,
                convite.getTreinador().getNome(),
                convite.getEquipa().getNome(),
                estado(convite),
                convite.getCriadoEm(),
                convite.getExpiraEm(),
                convite.getUsadoEm(),
                envio == null ? null : envio.name(),
                convite.getEnviadoPara()
        );
    }

    /**
     * A partir do registo da emissão, que é o que sobra depois de a transação
     * que emitiu ter fechado. Um convite acabado de emitir está sempre
     * disponível — foi o serviço que o garantiu ao devolvê-lo.
     */
    public static ConviteTreinadorDto deEmissao(EmissaoDeConvitesService.PorEntregar emitido,
                                                String link,
                                                ConviteTreinadorService.Envio envio) {
        return new ConviteTreinadorDto(
                emitido.conviteId(),
                emitido.codigo(),
                link,
                emitido.treinador(),
                emitido.equipa(),
                "DISPONIVEL",
                emitido.criadoEm(),
                emitido.expiraEm(),
                null,
                envio == null ? null : envio.name(),
                envio == ConviteTreinadorService.Envio.ENVIADO ? emitido.email() : null
        );
    }

    private static String estado(ConviteTreinador convite) {
        if (convite.estaUsado()) return "USADO";
        if (convite.estaRevogado()) return "REVOGADO";
        if (convite.estaExpirado()) return "EXPIRADO";
        return "DISPONIVEL";
    }
}
