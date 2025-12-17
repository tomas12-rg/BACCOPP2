package com.baccosoft.baccosoft.Controladores;

import com.baccosoft.baccosoft.Entidades.Caja;
import com.baccosoft.baccosoft.Entidades.Venta;
import com.baccosoft.baccosoft.Repositorios.CajaRepositorio;
import com.baccosoft.baccosoft.Repositorios.VentaRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/caja")
@CrossOrigin(origins = "*")
public class CajaController {

    @Autowired
    private CajaRepositorio cajaRepositorio;

    // ✅ AGREGAR ESTA INYECCIÓN
    @Autowired
    private VentaRepositorio ventaRepositorio;

    @GetMapping("/estado")
    public ResponseEntity<?> obtenerEstadoCaja() {
        try {
            System.out.println("=== VERIFICAR ESTADO DE CAJA ===");
            
            Optional<Caja> cajaOpt = cajaRepositorio.findTopByOrderByIdDesc();
            
            if (cajaOpt.isEmpty()) {
                System.out.println("❌ No hay cajas registradas");
                Map<String, Object> response = new HashMap<>();
                response.put("abierta", false);
                response.put("mensaje", "No hay cajas registradas");
                return ResponseEntity.ok(response);
            }
            
            Caja ultimaCaja = cajaOpt.get();
            boolean estaAbierta = ultimaCaja.getFechaCierre() == null;
            
            System.out.println("✅ Última caja ID: " + ultimaCaja.getId());
            System.out.println("📅 Fecha apertura: " + ultimaCaja.getFechaApertura());
            System.out.println("📅 Fecha cierre: " + ultimaCaja.getFechaCierre());
            System.out.println("🔓 Estado: " + (estaAbierta ? "ABIERTA" : "CERRADA"));
            
            Map<String, Object> response = new HashMap<>();
            response.put("abierta", estaAbierta);
            response.put("caja", ultimaCaja);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ Error al verificar estado de caja: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "error", "ERROR_ESTADO_CAJA",
                    "mensaje", "Error al verificar estado: " + e.getMessage()
                ));
        }
    }

    @PostMapping("/abrir")
    public ResponseEntity<?> abrirCaja(@RequestBody Map<String, Object> request) {
        try {
            Optional<Caja> cajaExistente = cajaRepositorio.findCajaAbierta();
            if (cajaExistente.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Ya existe una caja abierta");
                return ResponseEntity.badRequest().body(response);
            }

            Caja nuevaCaja = new Caja();
            nuevaCaja.setFechaApertura(LocalDateTime.now());
            
            Double montoInicial = request.get("montoInicial") != null 
                ? Double.parseDouble(request.get("montoInicial").toString()) 
                : 0.0;
            
            nuevaCaja.setMontoInicial(montoInicial);
            nuevaCaja.setMontoFinal(0.0);
            nuevaCaja.setTotalVentas(0.0);
            nuevaCaja.setCantidadVentas(0);
            
            String usuario = request.get("usuario") != null 
                ? request.get("usuario").toString() 
                : "Sistema";
            
            String observaciones = request.get("observaciones") != null 
                ? request.get("observaciones").toString() 
                : "";
            
            nuevaCaja.setUsuario(usuario);
            nuevaCaja.setObservaciones(observaciones);
            
            Caja cajaGuardada = cajaRepositorio.save(nuevaCaja);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Caja abierta exitosamente con $" + montoInicial);
            response.put("caja", cajaGuardada);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error al abrir caja: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/cerrar")
public ResponseEntity<?> cerrarCaja(@RequestBody Map<String, Object> request) {
    try {
        System.out.println("=== INICIANDO CIERRE DE CAJA ===");
        
        Optional<Caja> cajaOpt = cajaRepositorio.findCajaAbierta();
        
        if (cajaOpt.isEmpty()) {
            System.out.println("❌ No hay ninguna caja abierta");
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "No hay ninguna caja abierta");
            return ResponseEntity.badRequest().body(response);
        }

        Caja caja = cajaOpt.get();
        System.out.println("📦 Caja encontrada ID: " + caja.getId());
        System.out.println("💵 Monto Inicial: $" + caja.getMontoInicial());
        
        // ✅ CALCULAR VENTAS DE LA CAJA
        List<Venta> ventasDeLaCaja = ventaRepositorio.findAll().stream()
            .filter(v -> {
                LocalDateTime fechaVenta = v.getFecha();
                return fechaVenta.isAfter(caja.getFechaApertura()) || 
                       fechaVenta.isEqual(caja.getFechaApertura());
            })
            .collect(Collectors.toList());

        System.out.println("🧾 Ventas encontradas: " + ventasDeLaCaja.size());

        // ✅ Calcular totales
        double totalVentas = ventasDeLaCaja.stream()
            .mapToDouble(Venta::getTotal)
            .sum();
        
        int cantidadVentas = ventasDeLaCaja.size();

        System.out.println("💰 Total Ventas: $" + totalVentas);
        System.out.println("📊 Cantidad Ventas: " + cantidadVentas);

        // ✅ ACTUALIZAR CAJA
        caja.setFechaCierre(LocalDateTime.now());
        caja.setTotalVentas(totalVentas);
        caja.setCantidadVentas(cantidadVentas);
        
        // ✅ MONTO FINAL = Monto Inicial + Total Ventas
        double montoFinal = caja.getMontoInicial() + totalVentas;
        caja.setMontoFinal(montoFinal);
        
        System.out.println("💵 Monto Final calculado: $" + montoFinal);
        
        // ✅ Actualizar observaciones si vienen
        if (request.get("observaciones") != null && !request.get("observaciones").toString().isEmpty()) {
            String observaciones = request.get("observaciones").toString();
            String obsActuales = caja.getObservaciones() != null ? caja.getObservaciones() : "";
            caja.setObservaciones(obsActuales + "\n[Cierre] " + observaciones);
        }

        Caja cajaCerrada = cajaRepositorio.save(caja);
        System.out.println("✅ Caja cerrada exitosamente");

        // ✅ RESPUESTA CORRECTA
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Caja cerrada exitosamente");
        response.put("caja", cajaCerrada);
        response.put("montoInicial", caja.getMontoInicial());
        response.put("totalVentas", totalVentas);
        response.put("montoFinal", montoFinal);
        response.put("cantidadVentas", cantidadVentas);
        response.put("diferencia", cajaCerrada.calcularDiferencia());
        
        System.out.println("=== CIERRE DE CAJA COMPLETADO ===");
        
        return ResponseEntity.ok(response);
        
    } catch (Exception e) {
        System.err.println("❌ ERROR AL CERRAR CAJA: " + e.getMessage());
        e.printStackTrace();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "Error al cerrar caja: " + e.getMessage());
        return ResponseEntity.status(500).body(response);
    }
}

    @GetMapping("/historial")
    public ResponseEntity<?> obtenerHistorial() {
        try {
            System.out.println("=== HISTORIAL DE CAJAS ===");
            List<Caja> cajas = cajaRepositorio.findAll();
            System.out.println("✅ Cajas encontradas: " + cajas.size());
            return ResponseEntity.ok(cajas);
        } catch (Exception e) {
            System.err.println("❌ Error al obtener historial: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "error", "ERROR_HISTORIAL",
                    "mensaje", "Error al obtener historial: " + e.getMessage()
                ));
        }
    }
}