package com.ligarecord.web.dto;

import java.util.List;
import java.util.UUID;

/**
 * A ordem das equipas empatadas, numa lista só. O servidor volta a arrumá-las
 * dentro do grupo de pontos a que pertencem, por isso isto nunca consegue
 * trocar equipas entre pontuações diferentes.
 */
public record ResolverDesempateGeralRequest(List<UUID> ordem) {
}
