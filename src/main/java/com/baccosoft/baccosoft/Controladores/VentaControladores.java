package com.baccosoft.baccosoft.Controladores;

import com.baccosoft.baccosoft.Entidades.Venta;
import com.baccosoft.baccosoft.Entidades.Caja;
import com.baccosoft.baccosoft.Enums.MetodoPago;
import com.baccosoft.baccosoft.Entidades.Producto;
import com.baccosoft.baccosoft.Repositorios.VentaRepositorio;
import com.baccosoft.baccosoft.Servicios.PagoServicio;
import com.baccosoft.baccosoft.Servicios.ExportacionServicio;
import com.baccosoft.baccosoft.Servicios.PagoServicio.ResultadoPago;
import com.baccosoft.baccosoft.Repositorios.CajaRepositorio;
import com.baccosoft.baccosoft.Repositorios.ProductoRepositorio;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/ventas")
@CrossOrigin(origins = "*")
public class VentaControladores {

    @Autowired
    private VentaRepositorio ventaRepositorio;

    @Autowired
    private ProductoRepositorio productoRepositorio;

    @Autowired
    private PagoServicio pagoServicio;

    @Autowired
    private ExportacionServicio exportacionServicio;

    @Autowired
    private CajaRepositorio cajaRepositorio;

    // DTOs
    public static class VentaRequest {
        private List<DetalleVentaRequest> detalles;
        private String metodoPago;
        private Double montoPagado;

        public List<DetalleVentaRequest> getDetalles() { return detalles; }
        public void setDetalles(List<DetalleVentaRequest> detalles) { this.detalles = detalles; }
        public String getMetodoPago() { return metodoPago; }
        public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }
        public Double getMontoPagado() { return montoPagado; }
        public void setMontoPagado(Double montoPagado) { this.montoPagado = montoPagado; }
    }

    public static class DetalleVentaRequest {
        private Long productoId;
        private Integer cantidad;

        public Long getProductoId() { return productoId; }
        public void setProductoId(Long productoId) { this.productoId = productoId; }
        public Integer getCantidad() { return cantidad; }
        public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
    }

    @PostMapping
    public ResponseEntity<?> crearVenta(@RequestBody VentaRequest ventaRequest) {
        try {
            System.out.println("=== CREAR VENTA ===");
            
            // ✅ VALIDACIÓN: Verificar que hay caja abierta
            Optional<Caja> cajaOpt = cajaRepositorio.findCajaAbierta();
            
            if (cajaOpt.isEmpty()) {
                System.err.println("❌ CAJA CERRADA - No se puede realizar la venta");
                return ResponseEntity.badRequest()
                    .body(Map.of(
                        "error", "CAJA_CERRADA",
                        "mensaje", "⚠️ No se puede realizar la venta. Debe abrir la caja primero."
                    ));
            }
            
            Caja cajaAbierta = cajaOpt.get();
            System.out.println("✅ Caja abierta - ID: " + cajaAbierta.getId());

            // ✅ Validar método de pago
            MetodoPago metodoPago;
            try {
                metodoPago = MetodoPago.valueOf(ventaRequest.getMetodoPago());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                    .body(Map.of(
                        "error", "METODO_PAGO_INVALIDO",
                        "mensaje", "Método de pago no válido: " + ventaRequest.getMetodoPago()
                    ));
            }
            
            // ✅ Validar stock ANTES de procesar
            for (DetalleVentaRequest detalle : ventaRequest.getDetalles()) {
                Producto producto = productoRepositorio.findById(detalle.getProductoId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + detalle.getProductoId()));
                
                if (producto.getStock() < detalle.getCantidad()) {
                    System.err.println("❌ Stock insuficiente: " + producto.getNombre());
                    return ResponseEntity.badRequest()
                        .body(Map.of(
                            "error", "STOCK_INSUFICIENTE",
                            "mensaje", "Stock insuficiente para: " + producto.getNombre() + 
                                  ". Disponible: " + producto.getStock() + ", Solicitado: " + detalle.getCantidad()
                        ));
                }
            }
            
            // ✅ Crear venta
            Venta venta = new Venta();
            venta.setMetodoPago(metodoPago.name());

            for (DetalleVentaRequest detalle : ventaRequest.getDetalles()) {
                Producto producto = productoRepositorio.findById(detalle.getProductoId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
                venta.agregarDetalle(producto, detalle.getCantidad());
            }

            // ✅ Calcular vuelto si es efectivo
            Double vuelto = 0.0;
            if (metodoPago == MetodoPago.EFECTIVO) {
                if (ventaRequest.getMontoPagado() == null || ventaRequest.getMontoPagado() < venta.getTotal()) {
                    return ResponseEntity.badRequest()
                        .body(Map.of(
                            "error", "MONTO_INSUFICIENTE",
                            "mensaje", "El monto pagado es insuficiente"
                        ));
                }
                vuelto = ventaRequest.getMontoPagado() - venta.getTotal();
            }

            // ✅ Procesar pago
            ResultadoPago resultado = pagoServicio.procesarPago(venta.getTotal(), metodoPago)
                .get(30, TimeUnit.SECONDS);

            if (!resultado.isExitoso()) {
                return ResponseEntity.badRequest()
                    .body(Map.of(
                        "error", "PAGO_RECHAZADO",
                        "mensaje", "Pago rechazado: " + resultado.getMensaje()
                    ));
            }

            // ✅ Guardar venta
            venta.setNumeroTransaccion(resultado.getNumeroTransaccion());
            ventaRepositorio.save(venta);
            
            // ✅ Actualizar stock
            venta.getDetalles().forEach(detalle -> {
                Producto producto = detalle.getProducto();
                int nuevoStock = producto.getStock() - detalle.getCantidad();
                producto.setStock(nuevoStock);
                productoRepositorio.save(producto);
                System.out.println("✅ Stock actualizado - " + producto.getNombre() + " | Nuevo: " + nuevoStock);
            });

            // ✅ Actualizar caja
            cajaAbierta.setTotalVentas(cajaAbierta.getTotalVentas() + venta.getTotal());
            cajaAbierta.setCantidadVentas(cajaAbierta.getCantidadVentas() + 1);
            cajaRepositorio.save(cajaAbierta);

            System.out.println("✅ Venta creada - ID: " + venta.getId() + " | Total: $" + venta.getTotal());

            // ✅ Respuesta exitosa
            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Venta realizada con éxito");
            response.put("id", venta.getId());
            response.put("total", venta.getTotal());
            response.put("numeroTransaccion", resultado.getNumeroTransaccion());
            response.put("fecha", venta.getFecha().toString());
            
            if (metodoPago == MetodoPago.EFECTIVO) {
                response.put("montoPagado", ventaRequest.getMontoPagado());
                response.put("vuelto", vuelto);
            }
            
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            System.err.println("❌ Error de validación: " + e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "error", "ERROR_VALIDACION",
                    "mensaje", e.getMessage()
                ));
        } catch (Exception e) {
            System.err.println("❌ Error al crear venta: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "error", "ERROR_VENTA",
                    "mensaje", "Error al procesar la venta: " + e.getMessage()
                ));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> modificarVenta(@PathVariable Long id, @RequestBody VentaRequest ventaRequest) {
        try {
            System.out.println("=== MODIFICAR VENTA ===");
            System.out.println("ID: " + id);
            
            Venta ventaExistente = ventaRepositorio.findById(id)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada"));

            Caja cajaAbierta = cajaRepositorio.findCajaAbierta()
                .orElseThrow(() -> new RuntimeException("No hay caja abierta"));

            // ✅ Restaurar stock de la venta anterior
            ventaExistente.getDetalles().forEach(detalle -> {
                Producto producto = detalle.getProducto();
                int stockRestaurado = producto.getStock() + detalle.getCantidad();
                producto.setStock(stockRestaurado);
                productoRepositorio.save(producto);
            });

            cajaAbierta.setTotalVentas(cajaAbierta.getTotalVentas() - ventaExistente.getTotal());
            cajaAbierta.setCantidadVentas(cajaAbierta.getCantidadVentas() - 1);

            // ✅ Validar stock para los nuevos productos
            for (DetalleVentaRequest detalle : ventaRequest.getDetalles()) {
                Producto producto = productoRepositorio.findById(detalle.getProductoId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
                
                if (producto.getStock() < detalle.getCantidad()) {
                    return ResponseEntity.badRequest()
                        .body(Map.of(
                            "error", "STOCK_INSUFICIENTE",
                            "mensaje", "Stock insuficiente para: " + producto.getNombre()
                        ));
                }
            }

            ventaExistente.getDetalles().clear();

            for (DetalleVentaRequest detalle : ventaRequest.getDetalles()) {
                Producto producto = productoRepositorio.findById(detalle.getProductoId())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
                ventaExistente.agregarDetalle(producto, detalle.getCantidad());
            }

            ventaExistente.setMetodoPago(ventaRequest.getMetodoPago());
            ventaRepositorio.save(ventaExistente);

            ventaExistente.getDetalles().forEach(detalle -> {
                Producto producto = detalle.getProducto();
                int nuevoStock = producto.getStock() - detalle.getCantidad();
                producto.setStock(nuevoStock);
                productoRepositorio.save(producto);
            });

            cajaAbierta.setTotalVentas(cajaAbierta.getTotalVentas() + ventaExistente.getTotal());
            cajaAbierta.setCantidadVentas(cajaAbierta.getCantidadVentas() + 1);
            cajaRepositorio.save(cajaAbierta);

            System.out.println("✅ Venta modificada - ID: " + id);

            return ResponseEntity.ok(Map.of(
                "mensaje", "Venta modificada exitosamente",
                "venta", ventaExistente
            ));

        } catch (Exception e) {
            System.err.println("❌ Error al modificar venta: " + e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "error", "ERROR_MODIFICACION",
                    "mensaje", "Error al modificar venta: " + e.getMessage()
                ));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarVenta(@PathVariable Long id) {
        try {
            System.out.println("=== ELIMINAR VENTA ===");
            System.out.println("ID: " + id);
            
            Venta venta = ventaRepositorio.findById(id)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada"));

            Caja cajaAbierta = cajaRepositorio.findCajaAbierta()
                .orElseThrow(() -> new RuntimeException("Debe abrir la caja antes de eliminar ventas"));

            // ✅ Restaurar stock
            venta.getDetalles().forEach(detalle -> {
                Producto producto = detalle.getProducto();
                int stockRestaurado = producto.getStock() + detalle.getCantidad();
                producto.setStock(stockRestaurado);
                productoRepositorio.save(producto);
                System.out.println("✅ Stock restaurado - " + producto.getNombre() + " | Nuevo: " + stockRestaurado);
            });

            cajaAbierta.setTotalVentas(cajaAbierta.getTotalVentas() - venta.getTotal());
            cajaAbierta.setCantidadVentas(cajaAbierta.getCantidadVentas() - 1);
            cajaRepositorio.save(cajaAbierta);

            ventaRepositorio.delete(venta);

            System.out.println("✅ Venta eliminada - ID: " + id);

            return ResponseEntity.ok(Map.of(
                "mensaje", "Venta eliminada exitosamente",
                "stockRestaurado", true
            ));

        } catch (Exception e) {
            System.err.println("❌ Error al eliminar venta: " + e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "error", "ERROR_ELIMINACION",
                    "mensaje", e.getMessage()
                ));
        }
    }

    @GetMapping
    public ResponseEntity<List<Venta>> listarVentas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
        
        System.out.println("=== LISTAR VENTAS ===");
        System.out.println("Desde: " + desde);
        System.out.println("Hasta: " + hasta);
        
        if (desde == null || hasta == null) {
            List<Venta> todasLasVentas = ventaRepositorio.findAll();
            System.out.println("✅ Total ventas (sin filtro): " + todasLasVentas.size());
            return ResponseEntity.ok(todasLasVentas);
        }
        
        List<Venta> ventasFiltradas = ventaRepositorio.findByFechaBetween(desde, hasta);
        System.out.println("✅ Ventas filtradas: " + ventasFiltradas.size());
        return ResponseEntity.ok(ventasFiltradas);
    }

    @GetMapping("/hoy")
    public ResponseEntity<List<Venta>> ventasHoy() {
        List<Venta> ventas = ventaRepositorio.findVentasHoy();
        System.out.println("✅ Ventas de hoy: " + ventas.size());
        return ResponseEntity.ok(ventas);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerVenta(@PathVariable Long id) {
        System.out.println("=== OBTENER VENTA ===");
        System.out.println("ID: " + id);
        
        return ventaRepositorio.findById(id)
            .map(venta -> {
                System.out.println("✅ Venta encontrada");
                return ResponseEntity.ok(venta);
            })
            .orElseGet(() -> {
                System.err.println("❌ Venta no encontrada");
                return ResponseEntity.notFound().build();
            });
    }

    @GetMapping("/exportar/pdf")
    public ResponseEntity<byte[]> exportarPDF(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
        try {
            System.out.println("=== EXPORTAR PDF ===");
            System.out.println("Desde: " + desde);
            System.out.println("Hasta: " + hasta);
            
            List<Venta> ventas;
            
            if (desde == null || hasta == null) {
                System.out.println("Exportando TODAS las ventas");
                ventas = ventaRepositorio.findAll();
            } else {
                System.out.println("Exportando ventas filtradas");
                ventas = ventaRepositorio.findByFechaBetween(desde, hasta);
            }
            
            System.out.println("Total ventas a exportar: " + ventas.size());
            
            if (ventas.isEmpty()) {
                System.out.println("⚠️ No hay ventas para exportar");
                return ResponseEntity.noContent().build();
            }
            
            byte[] pdf = exportacionServicio.exportarVentasPDF(ventas);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("filename", "ventas_" + 
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf");
            
            System.out.println("✅ PDF generado exitosamente: " + pdf.length + " bytes");
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(pdf);
                
        } catch (Exception e) {
            System.err.println("❌ ERROR al generar PDF: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/exportar/excel")
    public ResponseEntity<byte[]> exportarExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
        try {
            System.out.println("=== EXPORTAR EXCEL ===");
            System.out.println("Desde: " + desde);
            System.out.println("Hasta: " + hasta);
            
            List<Venta> ventas;
            
            if (desde == null || hasta == null) {
                System.out.println("Exportando TODAS las ventas");
                ventas = ventaRepositorio.findAll();
            } else {
                System.out.println("Exportando ventas filtradas");
                ventas = ventaRepositorio.findByFechaBetween(desde, hasta);
            }
            
            System.out.println("Total ventas a exportar: " + ventas.size());
            
            if (ventas.isEmpty()) {
                System.out.println("⚠️ No hay ventas para exportar");
                return ResponseEntity.noContent().build();
            }
            
            byte[] excel = exportacionServicio.exportarVentasExcel(ventas);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("filename", "ventas_" + 
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx");
            
            System.out.println("✅ Excel generado exitosamente: " + excel.length + " bytes");
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(excel);
                
        } catch (Exception e) {
            System.err.println("❌ ERROR al generar Excel: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}