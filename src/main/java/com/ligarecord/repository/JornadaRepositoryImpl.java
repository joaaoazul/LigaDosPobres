package com.ligarecord.repository;

import com.ligarecord.domain.Jornada;
import com.ligarecord.domain.Liga;
import com.ligarecord.domain.enums.EstadoJornada;

import java.util.ArrayList;
import java.util.List;

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

    @Override
    public List<Jornada> listarJornadas(Liga liga){ return jornadas;}

}
