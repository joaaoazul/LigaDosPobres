package com.ligarecord.repository.jpa;

import com.ligarecord.domain.ContaTreinador;
import com.ligarecord.domain.Convite;
import com.ligarecord.domain.ConviteTreinador;
import com.ligarecord.domain.Equipa;
import com.ligarecord.domain.Gestor;
import com.ligarecord.domain.Jornada;
import com.ligarecord.domain.Liga;
import com.ligarecord.domain.LigaLogo;
import com.ligarecord.domain.Treinador;
import com.ligarecord.repository.ContaTreinadorRepository;
import com.ligarecord.repository.ConviteRepository;
import com.ligarecord.repository.ConviteTreinadorRepository;
import com.ligarecord.repository.EquipaRepository;
import com.ligarecord.repository.GestorRepository;
import com.ligarecord.repository.JornadaRepository;
import com.ligarecord.repository.LigaLogoRepository;
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
    public static class LogosDeLiga implements LigaLogoRepository {

        private final LigaLogoJpaRepository jpa;

        public LogosDeLiga(LigaLogoJpaRepository jpa) {
            this.jpa = jpa;
        }

        @Override
        public void guardar(UUID ligaId, byte[] dados) {
            jpa.guardar(ligaId, dados);
        }

        @Override
        public Optional<byte[]> buscar(UUID ligaId) {
            return jpa.findById(ligaId).map(LigaLogo::getDados);
        }

        @Override
        public void apagar(UUID ligaId) {
            // Apagar uma liga sem logo é inócuo: o delete não afecta linha nenhuma.
            jpa.apagarPorLiga(ligaId);
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

        @Override
        public List<Gestor> listarTodos() {
            return jpa.findAllByOrderByCriadoEmAsc();
        }

        @Override
        public long contarAdminsAtivos() {
            return jpa.countByPapelAndAtivoTrue(com.ligarecord.domain.enums.PapelGestor.ADMIN);
        }
    }

    @Repository
    public static class ContasTreinador implements ContaTreinadorRepository {

        private final ContaTreinadorJpaRepository jpa;

        public ContasTreinador(ContaTreinadorJpaRepository jpa) {
            this.jpa = jpa;
        }

        @Override
        public ContaTreinador guardar(ContaTreinador conta) {
            return jpa.save(conta);
        }

        @Override
        public Optional<ContaTreinador> buscarPorEmail(String email) {
            return jpa.findByEmailIgnoreCase(email);
        }

        @Override
        public Optional<ContaTreinador> buscarPorId(UUID id) {
            return jpa.findById(id);
        }

        @Override
        public boolean existePorTreinador(UUID treinadorId) {
            return jpa.existsByTreinadorId(treinadorId);
        }
    }

    @Repository
    public static class ConvitesTreinador implements ConviteTreinadorRepository {

        private final ConviteTreinadorJpaRepository jpa;

        public ConvitesTreinador(ConviteTreinadorJpaRepository jpa) {
            this.jpa = jpa;
        }

        @Override
        public ConviteTreinador guardar(ConviteTreinador convite) {
            return jpa.save(convite);
        }

        @Override
        public Optional<ConviteTreinador> buscarPorCodigo(String codigo) {
            return jpa.findByCodigo(codigo);
        }

        @Override
        public Optional<ConviteTreinador> buscarPorIdEGestor(UUID id, UUID gestorId) {
            return jpa.findByIdAndCriadoPorId(id, gestorId);
        }

        @Override
        public List<ConviteTreinador> listarPorGestor(UUID gestorId) {
            return jpa.findByCriadoPorIdOrderByCriadoEmDesc(gestorId);
        }
    }

    @Repository
    public static class Convites implements ConviteRepository {

        private final ConviteJpaRepository jpa;

        public Convites(ConviteJpaRepository jpa) {
            this.jpa = jpa;
        }

        @Override
        public Convite guardar(Convite convite) {
            return jpa.save(convite);
        }

        @Override
        public Optional<Convite> buscarPorCodigo(String codigo) {
            return jpa.findByCodigo(codigo);
        }

        @Override
        public Optional<Convite> buscarPorId(UUID id) {
            return jpa.findById(id);
        }

        @Override
        public List<Convite> listarTodos() {
            return jpa.findAllByOrderByCriadoEmDesc();
        }
    }
}
