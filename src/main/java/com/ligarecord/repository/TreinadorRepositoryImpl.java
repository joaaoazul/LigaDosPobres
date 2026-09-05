package com.ligarecord.repository;

import com.ligarecord.domain.Gestor;
import com.ligarecord.domain.Treinador;

import java.util.List;

public class TreinadorRepositoryImpl extends RepositorioEmMemoria<Treinador> implements TreinadorRepository {

    @Override
    public Treinador guardar(Treinador treinador) {
        return super.guardar(treinador);
    }

    @Override
    public List<Treinador> buscarPorConta(Gestor conta) {
        return entidades.stream().filter(t -> conta.equals(t.getConta())).toList();
    }
}
