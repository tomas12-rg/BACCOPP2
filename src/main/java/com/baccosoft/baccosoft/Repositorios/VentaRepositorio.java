package com.baccosoft.baccosoft.Repositorios;

import com.baccosoft.baccosoft.Entidades.Venta;
import com.baccosoft.baccosoft.DTO.ChartDataDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VentaRepositorio extends JpaRepository<Venta, Long> {
    
    // ==========================================
    // CONSULTAS BÁSICAS
    // ==========================================
    
    /**
     * Busca ventas entre dos fechas (rango de fechas)
     * ✅ USADO EN: Filtro de fechas del frontend
     */
    List<Venta> findByFechaBetween(LocalDateTime inicio, LocalDateTime fin);
    
    /**
     * Obtiene todas las ventas del día actual
     * ✅ USADO EN: Dashboard y vista "Ventas de Hoy"
     * ✅ CORREGIDO: Compatible con PostgreSQL
     */
    @Query(value = "SELECT * FROM venta WHERE CAST(fecha AS DATE) = CURRENT_DATE", nativeQuery = true)
    List<Venta> findVentasHoy();
    
    /**
     * Obtiene todas las ventas ordenadas por fecha descendente
     */
    List<Venta> findByOrderByFechaDesc();
    
    /**
     * Busca ventas por método de pago específico
     * @param metodoPago EFECTIVO, TARJETA_CREDITO, TARJETA_DEBITO, TRANSFERENCIA
     */
    List<Venta> findByMetodoPago(String metodoPago);
    
    /**
     * Calcula el total de ventas del día actual
     * ✅ CORREGIDO: Compatible con PostgreSQL
     */
    @Query("SELECT COALESCE(SUM(v.total), 0.0) FROM Venta v WHERE CAST(v.fecha AS DATE) = CURRENT_DATE")
    Double findTotalVentasHoy();
    
    /**
     * Cuenta cuántas ventas se hicieron hoy
     * ✅ CORREGIDO: Compatible con PostgreSQL
     */
    @Query("SELECT COUNT(v) FROM Venta v WHERE CAST(v.fecha AS DATE) = CURRENT_DATE")
    Long countVentasHoy();

    // ==========================================
    // CONSULTAS PARA REPORTES Y GRÁFICOS
    // ==========================================

    /**
     * Productos más vendidos en un período
     * ✅ RETORNA: Lista de (nombre producto, cantidad vendida)
     * 📊 PARA: Gráfico de torta o ranking de productos
     */
    @Query("SELECT new com.baccosoft.baccosoft.DTO.ChartDataDTO(p.nombre, SUM(d.cantidad)) " +
           "FROM DetalleVenta d " +
           "JOIN d.producto p " +
           "JOIN d.venta v " +
           "WHERE v.fecha BETWEEN :fechaInicio AND :fechaFin " +
           "GROUP BY p.id, p.nombre " +
           "ORDER BY SUM(d.cantidad) DESC")
    List<ChartDataDTO> findProductosMasVendidos(
        @Param("fechaInicio") LocalDateTime fechaInicio,
        @Param("fechaFin") LocalDateTime fechaFin
    );

    /**
     * Ventas totales agrupadas por día
     * ✅ RETORNA: Lista de (fecha, total ventas del día)
     * 📊 PARA: Gráfico de barras de evolución diaria
     */
    @Query("SELECT new com.baccosoft.baccosoft.DTO.ChartDataDTO(" +
           "FUNCTION('TO_CHAR', v.fecha, 'DD/MM/YYYY'), " +
           "SUM(v.total)) " +
           "FROM Venta v " +
           "WHERE v.fecha BETWEEN :fechaInicio AND :fechaFin " +
           "GROUP BY FUNCTION('TO_CHAR', v.fecha, 'DD/MM/YYYY') " +
           "ORDER BY MIN(v.fecha)")
    List<ChartDataDTO> findVentasPorDia(
        @Param("fechaInicio") LocalDateTime fechaInicio,
        @Param("fechaFin") LocalDateTime fechaFin
    );

    /**
     * Ventas agrupadas por método de pago
     * ✅ RETORNA: Lista de (método de pago, total)
     * 📊 PARA: Gráfico de torta de métodos de pago
     */
    @Query("SELECT new com.baccosoft.baccosoft.DTO.ChartDataDTO(v.metodoPago, SUM(v.total)) " +
           "FROM Venta v " +
           "WHERE v.fecha BETWEEN :fechaInicio AND :fechaFin " +
           "GROUP BY v.metodoPago " +
           "ORDER BY SUM(v.total) DESC")
    List<ChartDataDTO> findVentasPorMetodoPago(
        @Param("fechaInicio") LocalDateTime fechaInicio,
        @Param("fechaFin") LocalDateTime fechaFin
    );

    /**
     * Ventas agrupadas por categoría de producto
     * ✅ RETORNA: Lista de (categoría, total ventas)
     * 📊 PARA: Gráfico de torta por categorías
     */
    @Query("SELECT new com.baccosoft.baccosoft.DTO.ChartDataDTO(p.categoria, SUM(d.cantidad * d.precioUnitario)) " +
           "FROM DetalleVenta d " +
           "JOIN d.producto p " +
           "JOIN d.venta v " +
           "WHERE v.fecha BETWEEN :fechaInicio AND :fechaFin " +
           "GROUP BY p.categoria " +
           "ORDER BY SUM(d.cantidad * d.precioUnitario) DESC")
    List<ChartDataDTO> findVentasPorCategoria(
        @Param("fechaInicio") LocalDateTime fechaInicio,
        @Param("fechaFin") LocalDateTime fechaFin
    );

    /**
     * Calcula el total de ventas en un período
     * ✅ USADO EN: Reportes y resúmenes financieros
     */
    @Query("SELECT COALESCE(SUM(v.total), 0.0) FROM Venta v " +
           "WHERE v.fecha BETWEEN :fechaInicio AND :fechaFin")
    Double findTotalVentas(
        @Param("fechaInicio") LocalDateTime fechaInicio,
        @Param("fechaFin") LocalDateTime fechaFin
    );

    /**
     * Cuenta cuántas ventas se realizaron en un período
     * ✅ USADO EN: Estadísticas y KPIs
     */
    @Query("SELECT COUNT(v) FROM Venta v " +
           "WHERE v.fecha BETWEEN :fechaInicio AND :fechaFin")
    Long findCantidadVentas(
        @Param("fechaInicio") LocalDateTime fechaInicio,
        @Param("fechaFin") LocalDateTime fechaFin
    );

    /**
     * Ticket promedio (precio promedio por venta)
     */
    @Query("SELECT COALESCE(AVG(v.total), 0.0) FROM Venta v " +
           "WHERE v.fecha BETWEEN :fechaInicio AND :fechaFin")
    Double findTicketPromedio(
        @Param("fechaInicio") LocalDateTime fechaInicio,
        @Param("fechaFin") LocalDateTime fechaFin
    );

    /**
     * Venta más alta del período
     */
    @Query("SELECT COALESCE(MAX(v.total), 0.0) FROM Venta v " +
           "WHERE v.fecha BETWEEN :fechaInicio AND :fechaFin")
    Double findVentaMasAlta(
        @Param("fechaInicio") LocalDateTime fechaInicio,
        @Param("fechaFin") LocalDateTime fechaFin
    );

    /**
     * Total de productos vendidos (cantidad de items)
     */
    @Query("SELECT COALESCE(SUM(d.cantidad), 0) FROM DetalleVenta d " +
           "JOIN d.venta v " +
           "WHERE v.fecha BETWEEN :fechaInicio AND :fechaFin")
    Long findTotalProductosVendidos(
        @Param("fechaInicio") LocalDateTime fechaInicio,
        @Param("fechaFin") LocalDateTime fechaFin
    );

    /**
     * Busca ventas por número de transacción
     * ✅ ÚTIL PARA: Búsqueda y tracking de pagos
     */
    Venta findByNumeroTransaccion(String numeroTransaccion);
}