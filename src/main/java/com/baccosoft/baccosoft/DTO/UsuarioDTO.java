package com.baccosoft.baccosoft.DTO;

public class UsuarioDTO {
    private Long id;
    private String username;
    private String rol;

    // Constructor
    public UsuarioDTO(Long id, String username, String rol) {
        this.id = id;
        this.username = username;
        this.rol = rol;
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    // ToString para debugging
    @Override
    public String toString() {
        return "UsuarioDTO{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", rol='" + rol + '\'' +
                '}';
    }
}