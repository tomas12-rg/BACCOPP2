package com.baccosoft.baccosoft.DTO;

import com.baccosoft.baccosoft.Entidades.Producto;

public class ProductoSimpleDTO {
    private Long id;
    private String nombre;
    private Double costo;
    private Double precio;
    private Double margen;

    public ProductoSimpleDTO() {
    }

    public ProductoSimpleDTO(Producto producto) {
        this.id = producto.getId();
        this.nombre = producto.getNombre();
        this.costo = producto.getCosto();
        this.precio = producto.getPrecio();
        this.margen = producto.getMargen();
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Double getCosto() {
        return costo;
    }

    public void setCosto(Double costo) {
        this.costo = costo;
    }

    public Double getPrecio() {
        return precio;
    }

    public void setPrecio(Double precio) {
        this.precio = precio;
    }

    public Double getMargen() {
        return margen;
    }

    public void setMargen(Double margen) {
        this.margen = margen;
    }
}