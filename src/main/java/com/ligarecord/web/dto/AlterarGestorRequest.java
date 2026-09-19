package com.ligarecord.web.dto;

/**
 * Cada pedido altera um campo: os outros vêm a null exceto o que se quer mudar.
 *
 * <p>{@code licencaDias}: autoriza a licença desta conta por esse número de
 * dias a partir de agora. {@code 0} (ou negativo) revoga-a de imediato — não
 * há um campo à parte para revogar, é só autorizar por zero dias.
 */
public record AlterarGestorRequest(
        Boolean ativo, String papel, Boolean podeCriarLigas, String email, String nome, Integer licencaDias) {
}
