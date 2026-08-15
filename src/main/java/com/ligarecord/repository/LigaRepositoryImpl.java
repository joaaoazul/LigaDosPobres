package com.ligarecord.repository;

import com.ligarecord.domain.Liga;

import java.util.ArrayList;
import java.util.List;

public class LigaRepositoryImpl implements LigaRepository {
    List<Liga> ligas = new ArrayList<>();


    @Override
    public Liga guardarLiga(Liga liga){
        boolean found = false;
for(int i = 0; i < ligas.size(); i++) {
    if (ligas.get(i).getId().equals(liga.getId())){
        ligas.set(i, liga);
        found = true;
        break;
    }
};

if(!found) {
        ligas.add(liga);
    }
        return liga;

    }

    @Override
    public List<Liga> listarLigas() {
        return ligas;
    }
}
