package com.baccosoft.baccosoft.Repositorios;

import com.baccosoft.baccosoft.Entidades.Caja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CajaRepositorio extends JpaRepository<Caja, Long> {
    
    // ✅ Buscar la última caja registrada
    Optional<Caja> findTopByOrderByIdDesc();
    
    // ✅ Buscar caja abierta (sin fecha de cierre)
    @Query("SELECT c FROM Caja c WHERE c.fechaCierre IS NULL")
    Optional<Caja> findCajaAbierta();
    
}