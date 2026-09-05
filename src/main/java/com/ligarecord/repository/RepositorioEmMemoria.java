package com.ligarecord.repository;

import com.ligarecord.domain.EntidadeBase;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Base dos repositórios em memória usados nos testes: guarda tudo numa lista
 * e faz upsert por id. Sem isto, cada repositório repetia o mesmo ciclo
 * "percorrer, substituir se encontrar, senão acrescentar".
 */
public abstract class RepositorioEmMemoria<T extends EntidadeBase> {

    protected final List<T> entidades = new ArrayList<>();

    protected T guardar(T entidade) {
        for (int i = 0; i < entidades.size(); i++) {
            if (entidades.get(i).getId().equals(entidade.getId())) {
                entidades.set(i, entidade);
                return entidade;
            }
        }
        entidades.add(entidade);
        return entidade;
    }

    protected Optional<T> buscarPorId(UUID id) {
        return entidades.stream().filter(e -> e.getId().equals(id)).findFirst();
    }
}
