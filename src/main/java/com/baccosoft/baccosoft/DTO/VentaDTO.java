package com.baccosoft.baccosoft.DTO;

import java.util.List;
import java.util.stream.Collectors;
import com.baccosoft.baccosoft.Entidades.Venta;

public class VentaDTO {
    private Long id;
    private String fecha;
    private String numeroTransaccion;
    private String metodoPago;
    private Double total;
    private List<DetalleVentaDTO> detalles;

    public VentaDTO() {
    }

    public VentaDTO(Venta venta) {
        this.id = venta.getId();
        this.fecha = venta.getFecha().toString();
        this.numeroTransaccion = venta.getNumeroTransaccion();
        this.metodoPago = venta.getMetodoPago();
        this.total = venta.getTotal();
        this.detalles = venta.getDetalles().stream()
            .map(DetalleVentaDTO::new)
            .collect(Collectors.toList());
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getNumeroTransaccion() {
        return numeroTransaccion;
    }

    public void setNumeroTransaccion(String numeroTransaccion) {
        this.numeroTransaccion = numeroTransaccion;
    }

    public String getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(String metodoPago) {
        this.metodoPago = metodoPago;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public List<DetalleVentaDTO> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetalleVentaDTO> detalles) {
        this.detalles = detalles;
    }
}