package com.ligarecord.security;

import com.ligarecord.domain.Gestor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * O gestor autenticado, tal como o Spring Security o vê. Guarda o id para que
 * cada consulta possa ser filtrada pelo dono sem ir outra vez à base de dados.
 */
public class GestorAutenticado implements UserDetails {

    private final UUID id;
    private final String email;
    private final String passwordHash;
    private final String nome;
    private final boolean admin;
    private final boolean ativo;
    private final boolean podeCriarLigas;

    public GestorAutenticado(Gestor gestor) {
        this.id = gestor.getId();
        this.email = gestor.getEmail();
        this.passwordHash = gestor.getPasswordHash();
        this.nome = gestor.getNome();
        this.admin = gestor.isAdmin();
        this.ativo = gestor.isAtivo();
        this.podeCriarLigas = gestor.isPodeCriarLigas();
    }

    public boolean isAdmin() {
        return admin;
    }

    public boolean isPodeCriarLigas() {
        return podeCriarLigas;
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> autoridades = new ArrayList<>();
        if (admin) {
            autoridades.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        // Um administrador pode sempre criar ligas, mesmo que a própria conta
        // não tenha a permissão marcada explicitamente.
        if (admin || podeCriarLigas) {
            autoridades.add(new SimpleGrantedAuthority("PODE_CRIAR_LIGAS"));
        }
        return autoridades;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return ativo;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /** Uma conta desativada não consegue autenticar-se. */
    @Override
    public boolean isEnabled() {
        return ativo;
    }
}
