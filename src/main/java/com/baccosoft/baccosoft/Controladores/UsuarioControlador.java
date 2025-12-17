package com.baccosoft.baccosoft.Controladores;

import com.baccosoft.baccosoft.Entidades.Usuario;
import com.baccosoft.baccosoft.Repositorios.UsuarioRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*")
public class UsuarioControlador {

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        Optional<Usuario> usuarioOpt = usuarioRepositorio.findByUsername(username);
        
        if (!usuarioOpt.isPresent()) {
            return ResponseEntity.status(401)
                .body(Map.of("error", "Credenciales inválidas"));
        }
        
        Usuario usuario = usuarioOpt.get();

        if (!usuario.getPassword().equals(password)) {
            return ResponseEntity.status(401)
                .body(Map.of("error", "Credenciales inválidas"));
        }

        if (!usuario.getActivo()) {
            return ResponseEntity.status(403)
                .body(Map.of("error", "Usuario desactivado"));
        }

        // Actualizar último acceso
        usuario.setUltimoAcceso(LocalDateTime.now());
        usuarioRepositorio.save(usuario);

        Map<String, Object> response = new HashMap<>();
        response.put("id", usuario.getId());
        response.put("username", usuario.getUsername());
        response.put("rol", usuario.getRol().toString());
        response.put("nombre", usuario.getNombre());
        response.put("email", usuario.getEmail());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> userData) {
        try {
            if (usuarioRepositorio.findByUsername(userData.get("username")).isPresent()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "El usuario ya existe"));
            }

            Usuario usuario = new Usuario();
            usuario.setUsername(userData.get("username"));
            usuario.setPassword(userData.get("password"));
            usuario.setNombre(userData.get("nombre"));
            usuario.setEmail(userData.get("email"));
            
            if (userData.containsKey("rol")) {
                usuario.setRol(Usuario.Rol.valueOf(userData.get("rol")));
            }

            usuarioRepositorio.save(usuario);

            return ResponseEntity.ok(Map.of(
                "mensaje", "Usuario registrado exitosamente",
                "id", usuario.getId()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Error al registrar usuario: " + e.getMessage()));
        }
    }

    @GetMapping("/all")
    public ResponseEntity<List<Usuario>> getAllUsuarios() {
        return ResponseEntity.ok(usuarioRepositorio.findAll());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUsuario(@PathVariable Long id, @RequestBody Map<String, String> userData) {
        try {
            Usuario usuario = usuarioRepositorio.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            if (userData.containsKey("nombre")) {
                usuario.setNombre(userData.get("nombre"));
            }
            if (userData.containsKey("email")) {
                usuario.setEmail(userData.get("email"));
            }
            if (userData.containsKey("password")) {
                usuario.setPassword(userData.get("password"));
            }
            if (userData.containsKey("rol")) {
                usuario.setRol(Usuario.Rol.valueOf(userData.get("rol")));
            }
            if (userData.containsKey("activo")) {
                usuario.setActivo(Boolean.parseBoolean(userData.get("activo")));
            }

            usuarioRepositorio.save(usuario);

            return ResponseEntity.ok(Map.of("mensaje", "Usuario actualizado exitosamente"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Error al actualizar usuario: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUsuario(@PathVariable Long id) {
        try {
            Usuario usuario = usuarioRepositorio.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            usuarioRepositorio.delete(usuario);

            return ResponseEntity.ok(Map.of("mensaje", "Usuario eliminado exitosamente"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Error al eliminar usuario: " + e.getMessage()));
        }
    }
}