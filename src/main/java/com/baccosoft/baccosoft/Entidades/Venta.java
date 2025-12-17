package com.baccosoft.baccosoft.Entidades;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "venta")
public class Venta {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(nullable = false, unique = true)
    private String numeroTransaccion;

    @Column(nullable = false)
    private String metodoPago;

    @Column(nullable = false)
    private Double total;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<DetalleVenta> detalles = new ArrayList<>();

    // Constructor por defecto
    public Venta() {
        this.fecha = LocalDateTime.now();
        this.total = 0.0;
    }

    // Constructor con parámetros básicos
    public Venta(String numeroTransaccion, String metodoPago) {
        this();
        this.numeroTransaccion = numeroTransaccion;
        this.setMetodoPago(metodoPago);
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
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
        if (metodoPago != null && !metodoPago.isEmpty()) {
            this.metodoPago = metodoPago.substring(0, 1).toUpperCase() + 
                            metodoPago.substring(1).toLowerCase();
        } else {
            this.metodoPago = metodoPago;
        }
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public List<DetalleVenta> getDetalles() {
        return new ArrayList<>(detalles);
    }

    // Métodos de gestión de detalles
    public void agregarDetalle(Producto producto, Integer cantidad) {
        if (producto == null) {
            throw new IllegalArgumentException("El producto no puede ser null");
        }
        if (cantidad == null || cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que 0");
        }

        DetalleVenta detalleExistente = buscarDetallePorProducto(producto);

        if (detalleExistente != null) {
            detalleExistente.setCantidad(detalleExistente.getCantidad() + cantidad);
        } else {
            DetalleVenta detalle = new DetalleVenta();
            detalle.setProducto(producto);
            detalle.setCantidad(cantidad);
            detalle.setPrecioUnitario(producto.getPrecio());
            detalle.setVenta(this);
            this.detalles.add(detalle);
        }
        
        recalcularTotal();
    }

    public void addDetalle(DetalleVenta detalle) {
        if (detalle != null) {
            detalles.add(detalle);
            detalle.setVenta(this);
            recalcularTotal();
        }
    }

    public void removeDetalle(DetalleVenta detalle) {
        if (detalle != null && detalles.remove(detalle)) {
            detalle.setVenta(null);
            recalcularTotal();
        }
    }

    public void clearDetalles() {
        detalles.forEach(detalle -> detalle.setVenta(null));
        detalles.clear();
        recalcularTotal();
    }

    public boolean tieneDetalles() {
        return !detalles.isEmpty();
    }

    public int getCantidadDetalles() {
        return detalles.size();
    }

    public DetalleVenta buscarDetallePorProducto(Producto producto) {
        if (producto == null) return null;
        return detalles.stream()
                .filter(d -> d.getProducto().getId().equals(producto.getId()))
                .findFirst()
                .orElse(null);
    }

    // Método para recalcular el total
    private void recalcularTotal() {
        this.total = detalles.stream()
                .filter(Objects::nonNull)
                .mapToDouble(detalle -> 
                    detalle.getCantidad() * detalle.getPrecioUnitario())
                .sum();
    }

    // Método toString para debugging
    @Override
    public String toString() {
        return "Venta{" +
                "id=" + id +
                ", fecha=" + fecha +
                ", numeroTransaccion='" + numeroTransaccion + '\'' +
                ", metodoPago='" + metodoPago + '\'' +
                ", total=" + total +
                ", detalles=" + detalles.size() +
                '}';
    }

    // Equals y HashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Venta)) return false;
        Venta venta = (Venta) o;
        return numeroTransaccion != null && numeroTransaccion.equals(venta.numeroTransaccion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(numeroTransaccion);
    }
}