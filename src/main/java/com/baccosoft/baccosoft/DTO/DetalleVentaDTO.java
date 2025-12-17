package com.baccosoft.baccosoft.DTO;

import com.baccosoft.baccosoft.Entidades.DetalleVenta;

public class DetalleVentaDTO {
    private Long id;
    private ProductoSimpleDTO producto;
    private Integer cantidad;
    private Double precioUnitario;
    private Double subtotal;

    public DetalleVentaDTO() {
    }

    public DetalleVentaDTO(DetalleVenta detalle) {
        this.id = detalle.getId();
        this.producto = new ProductoSimpleDTO(detalle.getProducto());
        this.cantidad = detalle.getCantidad();
        this.precioUnitario = detalle.getPrecioUnitario();
        this.subtotal = detalle.getSubtotal();
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ProductoSimpleDTO getProducto() {
        return producto;
    }

    public void setProducto(ProductoSimpleDTO producto) {
        this.producto = producto;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public Double getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(Double precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public Double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(Double subtotal) {
        this.subtotal = subtotal;
    }
}