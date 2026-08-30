package com.ligarecord.security;

import com.ligarecord.domain.Gestor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

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

    public GestorAutenticado(Gestor gestor) {
        this.id = gestor.getId();
        this.email = gestor.getEmail();
        this.passwordHash = gestor.getPasswordHash();
        this.nome = gestor.getNome();
        this.admin = gestor.isAdmin();
        this.ativo = gestor.isAtivo();
    }

    public boolean isAdmin() {
        return admin;
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
        return admin
                ? List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                : List.of();
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
