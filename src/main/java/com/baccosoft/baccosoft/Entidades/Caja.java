package com.baccosoft.baccosoft.Entidades;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cajas")
public class Caja {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private LocalDateTime fechaApertura;
    
    @Column
    private LocalDateTime fechaCierre;
    
    @Column(nullable = false)
    private Double montoInicial = 0.0;
    
    @Column
    private Double montoFinal = 0.0;
    
    @Column
    private Double totalVentas = 0.0;
    
    @Column
    private Integer cantidadVentas = 0;
    
    @Column(length = 100)
    private String usuario;
    
    @Column(length = 500)
    private String observaciones;

    // ========== CONSTRUCTORES ==========
    public Caja() {
        this.fechaApertura = LocalDateTime.now();
        this.montoInicial = 0.0;
        this.montoFinal = 0.0;
        this.totalVentas = 0.0;
        this.cantidadVentas = 0;
    }

    // ========== GETTERS Y SETTERS ==========
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getFechaApertura() {
        return fechaApertura;
    }

    public void setFechaApertura(LocalDateTime fechaApertura) {
        this.fechaApertura = fechaApertura;
    }

    public LocalDateTime getFechaCierre() {
        return fechaCierre;
    }

    public void setFechaCierre(LocalDateTime fechaCierre) {
        this.fechaCierre = fechaCierre;
    }

    public Double getMontoInicial() {
        return montoInicial;
    }

    public void setMontoInicial(Double montoInicial) {
        this.montoInicial = montoInicial;
    }

    public Double getMontoFinal() {
        return montoFinal;
    }

    public void setMontoFinal(Double montoFinal) {
        this.montoFinal = montoFinal;
    }

    public Double getTotalVentas() {
        return totalVentas;
    }

    public void setTotalVentas(Double totalVentas) {
        this.totalVentas = totalVentas;
    }

    public Integer getCantidadVentas() {
        return cantidadVentas;
    }

    public void setCantidadVentas(Integer cantidadVentas) {
        this.cantidadVentas = cantidadVentas;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    // ========== MÉTODOS AUXILIARES ==========
    public boolean estaAbierta() {
        return this.fechaCierre == null;
    }

    public Double calcularDiferencia() {
        if (montoFinal == null) return 0.0;
        return montoFinal - (montoInicial + totalVentas);
    }

    @Override
    public String toString() {
        return "Caja{" +
                "id=" + id +
                ", fechaApertura=" + fechaApertura +
                ", fechaCierre=" + fechaCierre +
                ", montoInicial=" + montoInicial +
                ", montoFinal=" + montoFinal +
                ", totalVentas=" + totalVentas +
                ", cantidadVentas=" + cantidadVentas +
                ", usuario='" + usuario + '\'' +
                ", estaAbierta=" + estaAbierta() +
                '}';
    }
}