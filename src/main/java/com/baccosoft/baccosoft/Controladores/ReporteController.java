package com.baccosoft.baccosoft.Controladores;

import com.baccosoft.baccosoft.Entidades.Venta;
import com.baccosoft.baccosoft.Repositorios.VentaRepositorio;
import com.baccosoft.baccosoft.Servicios.ExportacionServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reportes")
@CrossOrigin(origins = "*")
public class ReporteController {

    @Autowired
    private VentaRepositorio ventaRepositorio;

    @Autowired
    private ExportacionServicio exportacionServicio;

    @GetMapping("/ventas")
    public ResponseEntity<?> obtenerVentas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        
        try {
            System.out.println("=== OBTENER VENTAS ===");
            System.out.println("Fecha Inicio: " + fechaInicio);
            System.out.println("Fecha Fin: " + fechaFin);
            
            List<Venta> ventas;

            if (fechaInicio != null && fechaFin != null) {
                LocalDateTime inicio = fechaInicio.atStartOfDay();
                LocalDateTime fin = fechaFin.atTime(LocalTime.MAX);
                System.out.println("Buscando desde: " + inicio + " hasta: " + fin);
                ventas = ventaRepositorio.findByFechaBetween(inicio, fin);
            } else {
                System.out.println("Obteniendo TODAS las ventas");
                ventas = ventaRepositorio.findAll();
            }

            System.out.println("Ventas encontradas: " + ventas.size());
            return ResponseEntity.ok(ventas);
        } catch (Exception e) {
            System.err.println("❌ ERROR: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error al obtener ventas: " + e.getMessage());
        }
    }

    @GetMapping("/ventas/por-dia")
    public ResponseEntity<?> obtenerVentasPorDia(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        
        try {
            System.out.println("=== VENTAS POR DIA ===");
            LocalDateTime inicio = fechaInicio.atStartOfDay();
            LocalDateTime fin = fechaFin.atTime(LocalTime.MAX);
            
            System.out.println("Rango: " + inicio + " a " + fin);
            
            List<Venta> ventas = ventaRepositorio.findByFechaBetween(inicio, fin);
            System.out.println("Ventas encontradas: " + ventas.size());

            Map<LocalDate, Double> ventasPorDia = ventas.stream()
                    .collect(Collectors.groupingBy(
                            v -> v.getFecha().toLocalDate(),
                            Collectors.summingDouble(Venta::getTotal)
                    ));

            List<Map<String, Object>> resultado = new ArrayList<>();
            for (Map.Entry<LocalDate, Double> entry : ventasPorDia.entrySet()) {
                Map<String, Object> dato = new HashMap<>();
                dato.put("fecha", entry.getKey().toString());
                dato.put("total", entry.getValue());
                resultado.add(dato);
            }

            resultado.sort((a, b) -> ((String)a.get("fecha")).compareTo((String)b.get("fecha")));

            System.out.println("✅ Días con ventas: " + resultado.size());
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            System.err.println("❌ ERROR: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error al obtener ventas por día: " + e.getMessage());
        }
    }

    @GetMapping("/grafico/ventas-por-periodo")
    public ResponseEntity<?> obtenerGraficoVentasPorPeriodo(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        
        try {
            System.out.println("=== GRAFICO VENTAS POR PERIODO ===");
            System.out.println("📅 Fecha Inicio: " + fechaInicio);
            System.out.println("📅 Fecha Fin: " + fechaFin);
            
            LocalDateTime inicio = fechaInicio.atStartOfDay();
            LocalDateTime fin = fechaFin.atTime(23, 59, 59);
            
            System.out.println("🕐 Inicio DateTime: " + inicio);
            System.out.println("🕐 Fin DateTime: " + fin);
            
            List<Venta> ventas = ventaRepositorio.findByFechaBetween(inicio, fin);
            System.out.println("📊 Ventas encontradas: " + ventas.size());

            if (ventas.isEmpty()) {
                System.out.println("⚠️ No se encontraron ventas en el rango especificado");
                Map<String, Object> resultadoVacio = new HashMap<>();
                resultadoVacio.put("labels", new ArrayList<>());
                resultadoVacio.put("data", new ArrayList<>());
                resultadoVacio.put("totalPeriodo", 0.0);
                resultadoVacio.put("cantidadVentas", 0);
                return ResponseEntity.ok(resultadoVacio);
            }

            Map<LocalDate, Double> ventasPorDia = ventas.stream()
                .collect(Collectors.groupingBy(
                    v -> v.getFecha().toLocalDate(),
                    TreeMap::new,
                    Collectors.summingDouble(Venta::getTotal)
                ));

            System.out.println("📆 Días con ventas: " + ventasPorDia.keySet());

            List<String> labels = new ArrayList<>();
            List<Double> data = new ArrayList<>();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            
            ventasPorDia.forEach((fecha, total) -> {
                labels.add(fecha.format(formatter));
                data.add(total);
                System.out.println("  📍 " + fecha + " -> $" + String.format("%,.2f", total));
            });

            double totalPeriodo = data.stream().mapToDouble(Double::doubleValue).sum();

            Map<String, Object> resultado = new HashMap<>();
            resultado.put("labels", labels);
            resultado.put("data", data);
            resultado.put("totalPeriodo", totalPeriodo);
            resultado.put("cantidadVentas", ventas.size());

            System.out.println("✅ Total período: $" + String.format("%,.2f", totalPeriodo));
            System.out.println("✅ Resultado enviado correctamente");
            
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            System.err.println("❌ ERROR al generar gráfico: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error al generar gráfico: " + e.getMessage());
        }
    }

    @GetMapping("/grafico/productos-mas-vendidos")
    public ResponseEntity<?> obtenerGraficoProductosMasVendidos(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(defaultValue = "10") int top) {
        
        try {
            System.out.println("=== GRAFICO PRODUCTOS MAS VENDIDOS ===");
            System.out.println("📅 Fecha Inicio: " + fechaInicio);
            System.out.println("📅 Fecha Fin: " + fechaFin);
            System.out.println("🔝 Top: " + top);
            
            LocalDateTime inicio = fechaInicio.atStartOfDay();
            LocalDateTime fin = fechaFin.atTime(23, 59, 59);
            
            List<Venta> ventas = ventaRepositorio.findByFechaBetween(inicio, fin);
            System.out.println("📊 Ventas encontradas: " + ventas.size());

            if (ventas.isEmpty()) {
                System.out.println("⚠️ No se encontraron ventas");
                Map<String, Object> resultadoVacio = new HashMap<>();
                resultadoVacio.put("labels", new ArrayList<>());
                resultadoVacio.put("data", new ArrayList<>());
                resultadoVacio.put("totalProductos", 0);
                resultadoVacio.put("cantidadTotal", 0);
                return ResponseEntity.ok(resultadoVacio);
            }

            Map<String, Integer> productosVendidos = new HashMap<>();

            ventas.forEach(venta -> {
                if (venta.getDetalles() != null && !venta.getDetalles().isEmpty()) {
                    venta.getDetalles().forEach(detalle -> {
                        if (detalle.getProducto() != null) {
                            String nombreProducto = detalle.getProducto().getNombre();
                            productosVendidos.merge(nombreProducto, detalle.getCantidad(), Integer::sum);
                        }
                    });
                }
            });

            System.out.println("🛒 Productos únicos encontrados: " + productosVendidos.size());

            List<Map.Entry<String, Integer>> topProductos = productosVendidos.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(top)
                .collect(Collectors.toList());

            List<String> labels = new ArrayList<>();
            List<Integer> data = new ArrayList<>();

            topProductos.forEach(entry -> {
                labels.add(entry.getKey());
                data.add(entry.getValue());
                System.out.println("  🍷 " + entry.getKey() + ": " + entry.getValue() + " unidades");
            });

            int cantidadTotal = data.stream().mapToInt(Integer::intValue).sum();

            Map<String, Object> resultado = new HashMap<>();
            resultado.put("labels", labels);
            resultado.put("data", data);
            resultado.put("totalProductos", productosVendidos.size());
            resultado.put("cantidadTotal", cantidadTotal);

            System.out.println("✅ Total unidades vendidas: " + cantidadTotal);
            System.out.println("✅ Resultado enviado correctamente");
            
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            System.err.println("❌ ERROR al generar gráfico: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error al generar gráfico: " + e.getMessage());
        }
    }

    @GetMapping("/productos-mas-vendidos")
    public ResponseEntity<?> obtenerProductosMasVendidos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        
        try {
            List<Venta> ventas;

            if (fechaInicio != null && fechaFin != null) {
                LocalDateTime inicio = fechaInicio.atStartOfDay();
                LocalDateTime fin = fechaFin.atTime(LocalTime.MAX);
                ventas = ventaRepositorio.findByFechaBetween(inicio, fin);
            } else {
                ventas = ventaRepositorio.findAll();
            }

            Map<String, Integer> productosVendidos = new HashMap<>();

            ventas.forEach(venta -> 
                venta.getDetalles().forEach(detalle -> {
                    String nombreProducto = detalle.getProducto().getNombre();
                    productosVendidos.merge(nombreProducto, detalle.getCantidad(), Integer::sum);
                })
            );

            List<Map<String, Object>> resultado = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : productosVendidos.entrySet()) {
                Map<String, Object> dato = new HashMap<>();
                dato.put("nombre", entry.getKey());
                dato.put("cantidad", entry.getValue());
                resultado.add(dato);
            }

            resultado.sort((a, b) -> ((Integer)b.get("cantidad")).compareTo((Integer)a.get("cantidad")));

            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al obtener productos más vendidos: " + e.getMessage());
        }
    }

    @GetMapping("/ventas/por-metodo-pago")
    public ResponseEntity<?> obtenerVentasPorMetodoPago(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        
        try {
            List<Venta> ventas;
            if (fechaInicio != null && fechaFin != null) {
                ventas = ventaRepositorio.findByFechaBetween(fechaInicio.atStartOfDay(), fechaFin.atTime(LocalTime.MAX));
            } else {
                ventas = ventaRepositorio.findAll();
            }

            Map<String, Double> ventasPorMetodo = ventas.stream()
                .collect(Collectors.groupingBy(
                    Venta::getMetodoPago,
                    Collectors.summingDouble(Venta::getTotal)
                ));

            List<Map<String, Object>> resultado = new ArrayList<>();
            for (Map.Entry<String, Double> entry : ventasPorMetodo.entrySet()) {
                Map<String, Object> dato = new HashMap<>();
                dato.put("metodoPago", entry.getKey());
                dato.put("total", entry.getValue());
                resultado.add(dato);
            }

            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/ventas/por-categoria")
    public ResponseEntity<?> obtenerVentasPorCategoria(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        
        try {
            List<Venta> ventas;
            if (fechaInicio != null && fechaFin != null) {
                ventas = ventaRepositorio.findByFechaBetween(fechaInicio.atStartOfDay(), fechaFin.atTime(LocalTime.MAX));
            } else {
                ventas = ventaRepositorio.findAll();
            }

            Map<String, Double> ventasPorCategoria = new HashMap<>();

            ventas.forEach(venta -> 
                venta.getDetalles().forEach(detalle -> {
                    String categoria = detalle.getProducto().getCategoria();
                    double totalDetalle = detalle.getPrecioUnitario() * detalle.getCantidad();
                    ventasPorCategoria.merge(categoria, totalDetalle, Double::sum);
                })
            );

            List<Map<String, Object>> resultado = new ArrayList<>();
            for (Map.Entry<String, Double> entry : ventasPorCategoria.entrySet()) {
                Map<String, Object> dato = new HashMap<>();
                dato.put("categoria", entry.getKey());
                dato.put("total", entry.getValue());
                resultado.add(dato);
            }

            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/resumen")
    public ResponseEntity<?> obtenerResumen(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        
        try {
            List<Venta> ventas;
            if (fechaInicio != null && fechaFin != null) {
                ventas = ventaRepositorio.findByFechaBetween(fechaInicio.atStartOfDay(), fechaFin.atTime(LocalTime.MAX));
            } else {
                ventas = ventaRepositorio.findAll();
            }

            double totalVentas = ventas.stream().mapToDouble(Venta::getTotal).sum();
            int cantidadVentas = ventas.size();
            double ticketPromedio = cantidadVentas > 0 ? totalVentas / cantidadVentas : 0;
            
            double ganancia = ventas.stream()
                .flatMap(venta -> venta.getDetalles().stream())
                .mapToDouble(detalle -> {
                    double precioVenta = detalle.getPrecioUnitario();
                    double precioCosto = detalle.getProducto().getCosto();
                    return (precioVenta - precioCosto) * detalle.getCantidad();
                })
                .sum();

            Map<String, Object> resumen = new HashMap<>();
            resumen.put("totalVentas", totalVentas);
            resumen.put("cantidadVentas", cantidadVentas);
            resumen.put("ticketPromedio", ticketPromedio);
            resumen.put("ganancia", ganancia);

            return ResponseEntity.ok(resumen);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * ✅ NUEVO: Exporta productos más vendidos a PDF
     */
    @GetMapping("/exportar/productos-mas-vendidos/pdf")
    public ResponseEntity<byte[]> exportarProductosMasVendidosPDF(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        
        try {
            System.out.println("📄 Generando PDF de productos más vendidos...");
            System.out.println("Desde: " + fechaInicio + " | Hasta: " + fechaFin);
            
            LocalDateTime inicio = fechaInicio.atStartOfDay();
            LocalDateTime fin = fechaFin.atTime(LocalTime.MAX);
            
            List<Venta> ventas = ventaRepositorio.findByFechaBetween(inicio, fin);
            System.out.println("✅ Ventas encontradas: " + ventas.size());
            
            byte[] pdfBytes = exportacionServicio.exportarProductosMasVendidosPDF(ventas);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "productos-mas-vendidos.pdf");
            headers.setContentLength(pdfBytes.length);
            
            System.out.println("✅ PDF generado: " + pdfBytes.length + " bytes");
            
            return ResponseEntity.ok().headers(headers).body(pdfBytes);
        } catch (Exception e) {
            System.err.println("❌ Error al generar PDF: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * ✅ NUEVO: Exporta productos más vendidos a Excel
     */
    @GetMapping("/exportar/productos-mas-vendidos/excel")
    public ResponseEntity<byte[]> exportarProductosMasVendidosExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        
        try {
            System.out.println("📊 Generando Excel de productos más vendidos...");
            System.out.println("Desde: " + fechaInicio + " | Hasta: " + fechaFin);
            
            LocalDateTime inicio = fechaInicio.atStartOfDay();
            LocalDateTime fin = fechaFin.atTime(LocalTime.MAX);
            
            List<Venta> ventas = ventaRepositorio.findByFechaBetween(inicio, fin);
            System.out.println("✅ Ventas encontradas: " + ventas.size());
            
            byte[] excelBytes = exportacionServicio.exportarProductosMasVendidosExcel(ventas);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "productos-mas-vendidos.xlsx");
            headers.setContentLength(excelBytes.length);
            
            System.out.println("✅ Excel generado: " + excelBytes.length + " bytes");
            
            return ResponseEntity.ok().headers(headers).body(excelBytes);
        } catch (Exception e) {
            System.err.println("❌ Error al generar Excel: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/exportar/resumen-ventas/pdf")
    public ResponseEntity<byte[]> exportarResumenVentasPDF(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        
        try {
            System.out.println("📄 Generando PDF de RESUMEN de ventas...");
            System.out.println("Desde: " + fechaInicio + " | Hasta: " + fechaFin);
            
            LocalDateTime inicio = fechaInicio.atStartOfDay();
            LocalDateTime fin = fechaFin.atTime(LocalTime.MAX);
            
            List<Venta> ventas = ventaRepositorio.findByFechaBetween(inicio, fin);
            System.out.println("✅ Ventas encontradas: " + ventas.size());
            
            // ✅ Llama al método de RESUMEN (sin detalle de productos)
            byte[] pdfBytes = exportacionServicio.exportarResumenVentasPDF(ventas);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "resumen-ventas.pdf");
            headers.setContentLength(pdfBytes.length);
            
            System.out.println("✅ PDF de resumen generado: " + pdfBytes.length + " bytes");
            
            return ResponseEntity.ok().headers(headers).body(pdfBytes);
        } catch (Exception e) {
            System.err.println("❌ Error al generar PDF de resumen: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }
}