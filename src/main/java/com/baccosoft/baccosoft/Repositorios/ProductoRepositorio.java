package com.baccosoft.baccosoft.Repositorios;

import com.baccosoft.baccosoft.Entidades.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductoRepositorio extends JpaRepository<Producto, Long> {
    
    // Buscar productos por nombre (ignorando mayúsculas/minúsculas)
    List<Producto> findByNombreContainingIgnoreCase(String nombre);
    
    // Buscar productos por tipo
    List<Producto> findByTipo(String tipo);

    List<Producto> findByActivoTrue();
    
    // Buscar productos por categoría
    List<Producto> findByCategoria(String categoria);
    
    // Buscar productos con stock bajo
    List<Producto> findByStockLessThan(Integer stock);
    
    // Búsqueda combinada por nombre, tipo o categoría
    @Query("SELECT p FROM Producto p WHERE " +
           "LOWER(p.nombre) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.tipo) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.categoria) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Producto> buscarPorTexto(@Param("query") String query);
}