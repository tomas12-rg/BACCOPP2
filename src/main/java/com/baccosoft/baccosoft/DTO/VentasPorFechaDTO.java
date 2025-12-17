package com.baccosoft.baccosoft.DTO;

import java.time.LocalDate;

public class VentasPorFechaDTO {
    private LocalDate fecha;
    private Double total;

    public VentasPorFechaDTO(LocalDate fecha, Double total) {
        this.fecha = fecha;
        this.total = total;
    }

    // Getters y Setters
    public LocalDate getFecha() { 
        return fecha; 
    }
    
    public void setFecha(LocalDate fecha) { 
        this.fecha = fecha; 
    }
    
    public Double getTotal() { 
        return total; 
    }
    
    public void setTotal(Double total) { 
        this.total = total; 
    }
}