package com.baccosoft.baccosoft.Servicios;

import com.baccosoft.baccosoft.DTO.ChartDataDTO;
import com.baccosoft.baccosoft.Repositorios.VentaRepositorio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class ReporteService {

    private final VentaRepositorio ventaRepositorio;
    private static final NumberFormat MONEY_FORMAT = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));

    public ReporteService(VentaRepositorio ventaRepositorio) {
        this.ventaRepositorio = ventaRepositorio;
    }

    /**
     * Formatea un valor numérico a moneda
     */
    public static String formatMoney(double value) {
        return MONEY_FORMAT.format(value);
    }

    /**
     * Obtiene productos más vendidos en un período
     */
    public List<ChartDataDTO> getProductosMasVendidos(LocalDate fechaInicio, LocalDate fechaFin) {
        LocalDateTime inicio = fechaInicio.atStartOfDay();
        LocalDateTime fin = fechaFin.atTime(LocalTime.MAX);
        
        System.out.println("📊 Productos más vendidos - Desde: " + inicio + " Hasta: " + fin);
        List<ChartDataDTO> resultado = ventaRepositorio.findProductosMasVendidos(inicio, fin);
        System.out.println("✅ Productos encontrados: " + resultado.size());
        
        return resultado;
    }

    /**
     * Obtiene ventas por día
     */
    public List<ChartDataDTO> getVentasPorDia(LocalDate fechaInicio, LocalDate fechaFin) {
        LocalDateTime inicio = fechaInicio.atStartOfDay();
        LocalDateTime fin = fechaFin.atTime(LocalTime.MAX);
        
        System.out.println("📊 Ventas por día - Desde: " + inicio + " Hasta: " + fin);
        List<ChartDataDTO> resultado = ventaRepositorio.findVentasPorDia(inicio, fin);
        System.out.println("✅ Días con ventas: " + resultado.size());
        
        return resultado;
    }

    /**
     * Obtiene ventas por método de pago
     */
    public List<ChartDataDTO> getVentasPorMetodoPago(LocalDate fechaInicio, LocalDate fechaFin) {
        LocalDateTime inicio = fechaInicio.atStartOfDay();
        LocalDateTime fin = fechaFin.atTime(LocalTime.MAX);
        
        System.out.println("📊 Ventas por método - Desde: " + inicio + " Hasta: " + fin);
        List<ChartDataDTO> resultado = ventaRepositorio.findVentasPorMetodoPago(inicio, fin);
        System.out.println("✅ Métodos de pago: " + resultado.size());
        
        return resultado;
    }

    /**
     * Obtiene ventas por categoría
     */
    public List<ChartDataDTO> getVentasPorCategoria(LocalDate fechaInicio, LocalDate fechaFin) {
        LocalDateTime inicio = fechaInicio.atStartOfDay();
        LocalDateTime fin = fechaFin.atTime(LocalTime.MAX);
        
        System.out.println("📊 Ventas por categoría - Desde: " + inicio + " Hasta: " + fin);
        List<ChartDataDTO> resultado = ventaRepositorio.findVentasPorCategoria(inicio, fin);
        System.out.println("✅ Categorías: " + resultado.size());
        
        return resultado;
    }

    /**
     * Obtiene resumen general de ventas
     */
    public Map<String, Object> getResumenVentas(LocalDate fechaInicio, LocalDate fechaFin) {
        LocalDateTime inicio = fechaInicio.atStartOfDay();
        LocalDateTime fin = fechaFin.atTime(LocalTime.MAX);

        System.out.println("📊 Resumen ventas - Desde: " + inicio + " Hasta: " + fin);

        double totalVentas = ventaRepositorio.findTotalVentas(inicio, fin);
        Long cantidadVentas = ventaRepositorio.findCantidadVentas(inicio, fin);

        Map<String, Object> resumen = new HashMap<>();
        resumen.put("totalVentas", totalVentas);
        resumen.put("totalVentasFormateado", formatMoney(totalVentas));
        resumen.put("cantidadVentas", cantidadVentas);
        resumen.put("fechaInicio", fechaInicio);
        resumen.put("fechaFin", fechaFin);

        System.out.println("✅ Total: " + formatMoney(totalVentas) + " - Cantidad: " + cantidadVentas);

        return resumen;
    }

    /**
     * Obtiene todos los datos para el dashboard
     */
    public Map<String, Object> getDashboardCompleto(LocalDate fechaInicio, LocalDate fechaFin) {
        System.out.println("=== DASHBOARD COMPLETO ===");
        System.out.println("📅 Rango: " + fechaInicio + " a " + fechaFin);
        
        Map<String, Object> dashboard = new HashMap<>();
        
        dashboard.put("productosMasVendidos", getProductosMasVendidos(fechaInicio, fechaFin));
        dashboard.put("ventasPorDia", getVentasPorDia(fechaInicio, fechaFin));
        dashboard.put("ventasPorMetodoPago", getVentasPorMetodoPago(fechaInicio, fechaFin));
        dashboard.put("ventasPorCategoria", getVentasPorCategoria(fechaInicio, fechaFin));
        dashboard.put("resumen", getResumenVentas(fechaInicio, fechaFin));

        System.out.println("✅ Dashboard generado correctamente");
        
        return dashboard;
    }
}