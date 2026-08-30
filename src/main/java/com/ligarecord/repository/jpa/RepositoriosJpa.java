package com.ligarecord.repository.jpa;

import com.ligarecord.domain.Equipa;
import com.ligarecord.domain.Gestor;
import com.ligarecord.domain.Jornada;
import com.ligarecord.domain.Liga;
import com.ligarecord.domain.Treinador;
import com.ligarecord.repository.EquipaRepository;
import com.ligarecord.repository.GestorRepository;
import com.ligarecord.repository.JornadaRepository;
import com.ligarecord.repository.LigaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptadores entre as interfaces de repositório do domínio e o Spring Data.
 * Os serviços continuam a depender só das interfaces do domínio; o Hibernate
 * fica deste lado da fronteira.
 */
public final class RepositoriosJpa {

    private RepositoriosJpa() {
    }

    @Repository
    public static class Ligas implements LigaRepository {

        private final LigaJpaRepository jpa;

        public Ligas(LigaJpaRepository jpa) {
            this.jpa = jpa;
        }

        @Override
        public Liga guardarLiga(Liga liga) {
            return jpa.save(liga);
        }

        @Override
        public List<Liga> listarLigas(Gestor gestor) {
            return jpa.findByGestorIdOrderByNome(gestor.getId());
        }

        @Override
        public Optional<Liga> buscarPorIdEGestor(UUID id, UUID gestorId) {
            return jpa.findByIdAndGestorId(id, gestorId);
        }
    }

    @Repository
    public static class Equipas implements EquipaRepository {

        private final EquipaJpaRepository jpa;

        public Equipas(EquipaJpaRepository jpa) {
            this.jpa = jpa;
        }

        @Override
        public Equipa guardar(Equipa equipa) {
            return jpa.save(equipa);
        }

        @Override
        public List<Equipa> buscarPorTreinador(Treinador treinador) {
            return jpa.findByTreinador(treinador);
        }

        @Override
        public Optional<Equipa> buscarPorIdEGestor(UUID id, UUID gestorId) {
            return jpa.findByIdAndLigaGestorId(id, gestorId);
        }
    }

    @Repository
    public static class Jornadas implements JornadaRepository {

        private final JornadaJpaRepository jpa;

        public Jornadas(JornadaJpaRepository jpa) {
            this.jpa = jpa;
        }

        @Override
        public Jornada guardar(Jornada jornada) {
            return jpa.save(jornada);
        }

        @Override
        public List<Jornada> listarJornadas(Liga liga) {
            return jpa.findByLigaOrderByNumJornada(liga);
        }

        @Override
        public Optional<Jornada> buscarPorIdEGestor(UUID id, UUID gestorId) {
            return jpa.findByIdAndLigaGestorId(id, gestorId);
        }
    }

    @Repository
    public static class Gestores implements GestorRepository {

        private final GestorJpaRepository jpa;

        public Gestores(GestorJpaRepository jpa) {
            this.jpa = jpa;
        }

        @Override
        public Gestor guardar(Gestor gestor) {
            return jpa.save(gestor);
        }

        @Override
        public Optional<Gestor> buscarPorEmail(String email) {
            return jpa.findByEmailIgnoreCase(email);
        }

        @Override
        public Optional<Gestor> buscarPorId(UUID id) {
            return jpa.findById(id);
        }
    }
}
