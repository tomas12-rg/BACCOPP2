package com.baccosoft.baccosoft.Entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "producto")  // ⬅️ CAMBIO: sin S
public class Producto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String nombre;
    
    @Column(nullable = false)
    private Double precio = 0.0;
    
    @Column(nullable = false)
    private Double costo = 0.0;
    
    @Column(nullable = false)
    private Double margen = 0.0;
    
    @Column(nullable = false)
    private Integer stock = 0;

    @Column(name = "margen_ganancia")
    private Double margenGanancia;
    
    private String tipo;
    private String descripcion;
    private String categoria;

    @Column(nullable = false)
    private Boolean activo = true;

    @Transient
    private boolean noRecalcular = false;

    public Producto() {}

    // Getters básicos
    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public Double getCosto() { return costo; }
    public Double getPrecio() { return precio; }
    public Double getMargen() { return margen; }
    public Integer getStock() { return stock; }
    public String getTipo() { return tipo; }
    public String getDescripcion() { return descripcion; }
    public String getCategoria() { return categoria; }
    public Double getMargenGanancia() { return margenGanancia; }

    // Setters básicos
    public void setId(Long id) { this.id = id; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public void setMargenGanancia(Double margenGanancia) { this.margenGanancia = margenGanancia; }

    public Boolean getActivo() { 
        return activo; 
    }
    
    public void setActivo(Boolean activo) { 
        this.activo = activo;
    }

    public void setPrecioDirecto(Double precio) {
        if (precio == null || precio < 0) {
            throw new IllegalArgumentException("El precio debe ser válido");
        }
        if (precio > 999999.99) {
            throw new IllegalArgumentException("El precio excede el máximo permitido");
        }
        this.noRecalcular = true;
        this.precio = precio;
        this.noRecalcular = false;
    }

    public void setCosto(Double costo) {
        if (costo == null) {
            throw new IllegalArgumentException("El costo no puede ser nulo");
        }
        if (costo < 0) {
            throw new IllegalArgumentException("El costo no puede ser negativo");
        }
        this.costo = costo;
        
        if (!noRecalcular) {
            recalcularPrecio();
        }
    }

    public void setMargen(Double margen) {
        if (margen == null) {
            this.margen = 0.0;
            return;
        }
        if (margen < 0) {
            throw new IllegalArgumentException("El margen no puede ser negativo");
        }
        if (margen > 1000) {
            throw new IllegalArgumentException("El margen no puede superar el 1000%");
        }
        this.margen = margen;
        
        if (!noRecalcular) {
            recalcularPrecio();
        }
    }

    public void setPrecio(Double precio) {
        if (precio == null) {
            throw new IllegalArgumentException("El precio no puede ser nulo");
        }
        if (precio < 0) {
            throw new IllegalArgumentException("El precio no puede ser negativo");
        }
        if (precio > 999999.99) {
            throw new IllegalArgumentException("El precio excede el máximo permitido");
        }
        this.precio = precio;
    }

    public void setStock(Integer stock) {
        if (stock == null) {
            throw new IllegalArgumentException("El stock no puede ser nulo");
        }
        if (stock < 0) {
            throw new IllegalArgumentException("El stock no puede ser negativo");
        }
        this.stock = stock;
    }

    private void recalcularPrecio() {
        if (this.costo != null && this.margen != null && this.margen > 0) {
            Double precioCalculado = Math.round((this.costo * (1 + this.margen / 100)) * 100.0) / 100.0;
            if (precioCalculado > 999999.99) {
                throw new IllegalArgumentException("El precio calculado excede el máximo permitido");
            }
            this.precio = precioCalculado;
        }
    }

    // Establecer todos los valores sin recalcular
    public void establecerValoresSinRecalcular(Double costo, Double precio, Double margen, Integer stock) {
        this.noRecalcular = true;
        
        if (costo != null) this.costo = costo;
        if (precio != null) this.precio = precio;
        if (margen != null) this.margen = margen;
        if (stock != null) this.stock = stock;
        
        this.noRecalcular = false;
    }

    // Calcular y establecer el margen basado en costo y precio
    public void calcularMargenDesdePrecios() {
        if (this.costo != null && this.costo > 0 && this.precio != null) {
            this.margen = Math.round(((this.precio - this.costo) / this.costo * 100) * 100.0) / 100.0;
        }
    }
}