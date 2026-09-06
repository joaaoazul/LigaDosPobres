package com.ligarecord.web.dto;

import java.util.List;
import java.util.UUID;

/**
 * A ordem em que ficam as equipas empatadas de uma jornada, da melhor para a
 * pior. Tem de conter exactamente as equipas empatadas.
 */
public record ResolverDesempateRequest(List<UUID> ordem) {
}
