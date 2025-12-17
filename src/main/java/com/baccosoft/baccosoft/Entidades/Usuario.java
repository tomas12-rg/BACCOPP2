package com.baccosoft.baccosoft.Entidades;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String username;
    
    @Column(nullable = false)
    private String password;
    
    @Enumerated(EnumType.STRING)
    private Rol rol;
    
    private String nombre;
    private String email;
    private Boolean activo;
    private LocalDateTime fechaCreacion;
    private LocalDateTime ultimoAcceso;

    public enum Rol {
        ADMIN,
        VENDEDOR,
        SUPERVISOR
    }

    public Usuario() {
        this.activo = true;
        this.fechaCreacion = LocalDateTime.now();
        this.rol = Rol.VENDEDOR;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }
    
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
    
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    
    public LocalDateTime getUltimoAcceso() { return ultimoAcceso; }
    public void setUltimoAcceso(LocalDateTime ultimoAcceso) { this.ultimoAcceso = ultimoAcceso; }
}