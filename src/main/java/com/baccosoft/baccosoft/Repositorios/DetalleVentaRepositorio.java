package com.baccosoft.baccosoft.Repositorios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.baccosoft.baccosoft.Entidades.DetalleVenta;

@Repository
public interface DetalleVentaRepositorio extends JpaRepository<DetalleVenta, Long> {
    
    Long countByProductoId(Long productoId);
}