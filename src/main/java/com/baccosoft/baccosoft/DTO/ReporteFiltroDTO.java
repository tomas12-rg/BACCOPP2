package com.baccosoft.baccosoft.DTO;

import java.time.LocalDate;

public class ReporteFiltroDTO {
    private LocalDate fechaInicio;
    private LocalDate fechaFin;

    public ReporteFiltroDTO() {
    }

    public ReporteFiltroDTO(LocalDate fechaInicio, LocalDate fechaFin) {
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
    }

    // Getters y Setters
    public LocalDate getFechaInicio() { 
        return fechaInicio; 
    }
    
    public void setFechaInicio(LocalDate fechaInicio) { 
        this.fechaInicio = fechaInicio; 
    }
    
    public LocalDate getFechaFin() { 
        return fechaFin; 
    }
    
    public void setFechaFin(LocalDate fechaFin) { 
        this.fechaFin = fechaFin; 
    }
}