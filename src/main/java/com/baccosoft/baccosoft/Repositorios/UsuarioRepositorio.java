package com.baccosoft.baccosoft.Repositorios;

import com.baccosoft.baccosoft.Entidades.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UsuarioRepositorio extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsername(String username);
}