package com.baccosoft.baccosoft.Controladores;

import com.baccosoft.baccosoft.Repositorios.VentaRepositorio;
import com.baccosoft.baccosoft.Repositorios.ProductoRepositorio;
import com.baccosoft.baccosoft.Repositorios.CajaRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardControlador {

    @Autowired
    private VentaRepositorio ventaRepositorio;

    @Autowired
    private ProductoRepositorio productoRepositorio;

    @Autowired
    private CajaRepositorio cajaRepositorio;

    @GetMapping("/resumen")
    public ResponseEntity<?> obtenerResumen() {
        try {
            var ventasHoy = ventaRepositorio.findVentasHoy();
            var cajaAbierta = cajaRepositorio.findCajaAbierta();
            var productosStockBajo = productoRepositorio.findAll().stream()
                .filter(p -> p.getStock() < 10)
                .collect(Collectors.toList());

            double totalVentasHoy = ventasHoy.stream()
                .mapToDouble(v -> v.getTotal())
                .sum();

            int cantidadVentasHoy = ventasHoy.size();

            Map<String, Object> resumen = new HashMap<>();
            resumen.put("totalVentasHoy", totalVentasHoy);
            resumen.put("cantidadVentasHoy", cantidadVentasHoy);
            resumen.put("cajaAbierta", cajaAbierta.isPresent());
            resumen.put("productosStockBajo", productosStockBajo.size());
            
            if (cajaAbierta.isPresent()) {
                resumen.put("montoEnCaja", cajaAbierta.get().getMontoInicial() + cajaAbierta.get().getTotalVentas());
            }

            return ResponseEntity.ok(resumen);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Error al obtener resumen: " + e.getMessage()));
        }
    }

    @GetMapping("/ventas-por-metodo-pago")
    public ResponseEntity<?> ventasPorMetodoPago(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        try {
            LocalDateTime inicio = fecha.atStartOfDay();
            LocalDateTime fin = fecha.plusDays(1).atStartOfDay();
            
            var ventas = ventaRepositorio.findByFechaBetween(inicio, fin);
            
            Map<String, Double> ventasPorMetodo = ventas.stream()
                .collect(Collectors.groupingBy(
                    v -> v.getMetodoPago(),
                    Collectors.summingDouble(v -> v.getTotal())
                ));

            return ResponseEntity.ok(ventasPorMetodo);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Error al obtener ventas por método de pago: " + e.getMessage()));
        }
    }

    @GetMapping("/productos-mas-vendidos")
    public ResponseEntity<?> productosMasVendidos(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        try {
            LocalDateTime inicio = fechaInicio.atStartOfDay();
            LocalDateTime fin = fechaFin.plusDays(1).atStartOfDay();
            
            var ventas = ventaRepositorio.findByFechaBetween(inicio, fin);
            
            Map<String, Integer> productosVendidos = new HashMap<>();
            
            ventas.forEach(venta -> {
                venta.getDetalles().forEach(detalle -> {
                    String nombreProducto = detalle.getProducto().getNombre();
                    productosVendidos.merge(nombreProducto, detalle.getCantidad(), Integer::sum);
                });
            });

            List<Map<String, Object>> top5 = productosVendidos.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .map(entry -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("producto", entry.getKey());
                    item.put("cantidad", entry.getValue());
                    return item;
                })
                .collect(Collectors.toList());

            return ResponseEntity.ok(top5);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Error al obtener productos más vendidos: " + e.getMessage()));
        }
    }

    @GetMapping("/productos-stock-bajo")
    public ResponseEntity<?> productosStockBajo() {
        try {
            var productos = productoRepositorio.findAll().stream()
                .filter(p -> p.getStock() < 10)
                .map(p -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", p.getId());
                    item.put("nombre", p.getNombre());
                    item.put("stock", p.getStock());
                    item.put("categoria", p.getCategoria());
                    return item;
                })
                .collect(Collectors.toList());

            return ResponseEntity.ok(productos);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Error al obtener productos con stock bajo: " + e.getMessage()));
        }
    }

    @GetMapping("/ventas-ultimos-dias")
    public ResponseEntity<?> ventasUltimosDias(@RequestParam(defaultValue = "7") int dias) {
        try {
            LocalDate hoy = LocalDate.now();
            List<Map<String, Object>> ventasPorDia = new ArrayList<>();

            for (int i = dias - 1; i >= 0; i--) {
                LocalDate fecha = hoy.minusDays(i);
                LocalDateTime inicio = fecha.atStartOfDay();
                LocalDateTime fin = fecha.plusDays(1).atStartOfDay();
                
                var ventas = ventaRepositorio.findByFechaBetween(inicio, fin);
                double total = ventas.stream().mapToDouble(v -> v.getTotal()).sum();
                
                Map<String, Object> dia = new HashMap<>();
                dia.put("fecha", fecha.toString());
                dia.put("total", total);
                dia.put("cantidad", ventas.size());
                
                ventasPorDia.add(dia);
            }

            return ResponseEntity.ok(ventasPorDia);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Error al obtener ventas de últimos días: " + e.getMessage()));
        }
    }
}