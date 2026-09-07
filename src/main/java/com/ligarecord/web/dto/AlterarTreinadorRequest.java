package com.ligarecord.web.dto;

/**
 * O nome do lugar de treinador de uma equipa — o rótulo que o gestor lhe dá — e
 * o email para onde vai o convite. O {@code email} a vazio apaga o que lá
 * estivesse: nem todo o treinador tem um, e isso é um estado normal.
 */
public record AlterarTreinadorRequest(String nome, String email) {
}
