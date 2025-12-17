package com.baccosoft.baccosoft.Servicios;

import com.baccosoft.baccosoft.Entidades.DetalleVenta;
import com.baccosoft.baccosoft.Entidades.Producto;
import com.baccosoft.baccosoft.Entidades.Venta;
import com.baccosoft.baccosoft.Repositorios.ProductoRepositorio;
import com.baccosoft.baccosoft.Repositorios.VentaRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class VentaService {

    @Autowired
    private VentaRepositorio ventaRepositorio;

    @Autowired
    private ProductoRepositorio productoRepositorio;

    @Transactional
    public Venta crearVenta(Venta venta) {
        // Generar número de transacción único
        venta.setNumeroTransaccion(UUID.randomUUID().toString());
        venta.setFecha(LocalDateTime.now());

        double total = 0.0;

        // Procesar cada detalle de la venta
        for (DetalleVenta detalle : venta.getDetalles()) {
            Producto producto = productoRepositorio.findById(detalle.getProducto().getId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + detalle.getProducto().getId()));

            // ✅ VERIFICAR STOCK DISPONIBLE
            if (producto.getStock() < detalle.getCantidad()) {
                throw new RuntimeException("Stock insuficiente para el producto: " + producto.getNombre() + 
                                         ". Disponible: " + producto.getStock() + 
                                         ", Solicitado: " + detalle.getCantidad());
            }

            // ✅ DESCONTAR STOCK
            producto.setStock(producto.getStock() - detalle.getCantidad());
            productoRepositorio.save(producto);

            // Configurar precio y subtotal
            detalle.setPrecioUnitario(producto.getPrecio());
            detalle.setSubtotal(detalle.getCantidad() * producto.getPrecio());
            detalle.setVenta(venta);

            total += detalle.getSubtotal();
        }

        venta.setTotal(total);
        return ventaRepositorio.save(venta);
    }

    public List<Venta> obtenerTodasLasVentas() {
        return ventaRepositorio.findAll(Sort.by(Sort.Direction.DESC, "fecha"));
    }

    public List<Venta> obtenerVentasPorRango(LocalDateTime desde, LocalDateTime hasta) {
    return ventaRepositorio.findByFechaBetween(desde, hasta);
}

    public Venta obtenerVentaPorId(Long id) {
        return ventaRepositorio.findById(id)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada: " + id));
    }

    public List<Venta> obtenerVentasPorFecha(LocalDateTime inicio, LocalDateTime fin) {
        return ventaRepositorio.findByFechaBetween(inicio, fin);
    }
}