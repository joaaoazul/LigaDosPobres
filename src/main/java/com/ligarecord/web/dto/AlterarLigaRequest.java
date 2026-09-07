package com.ligarecord.web.dto;

/**
 * O que se pode mudar numa liga já criada. Campos a null ficam como estão — um
 * cliente que só quer mexer numa coisa não tem de reenviar as outras.
 */
public record AlterarLigaRequest(Boolean pontosTreinoContam) {
}
