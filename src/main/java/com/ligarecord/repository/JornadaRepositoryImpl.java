package com.ligarecord.repository;

import com.ligarecord.domain.Jornada;
import com.ligarecord.domain.Liga;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class JornadaRepositoryImpl implements JornadaRepository {
    List<Jornada> jornadas = new ArrayList<>();

    @Override
    public Jornada guardar(Jornada jornada){
       boolean found = false;
       for(int i = 0; i < jornadas.size(); i++){
           if(jornadas.get(i).getId().equals(jornada.getId())){
               jornadas.set(i, jornada);
               found = true;
               break;
           }
       }
       if(!found){
           jornadas.add(jornada);

       }
       return jornada;
    }

    /**
     * Devolve apenas as jornadas guardadas que pertencem a esta liga.
     */
    @Override
    public List<Jornada> listarJornadas(Liga liga){
        List<Jornada> resultado = new ArrayList<>();
        if (liga == null) {
            return resultado;
        }
        for (Jornada jornada : jornadas) {
            if (liga.getJornadas().contains(jornada)) {
                resultado.add(jornada);
            }
        }
        return resultado;
    }

    @Override
    public Optional<Jornada> buscarPorId(UUID id) {
        for (Jornada jornada : jornadas) {
            if (jornada.getId().equals(id)) {
                return Optional.of(jornada);
            }
        }
        return Optional.empty();
    }
}
