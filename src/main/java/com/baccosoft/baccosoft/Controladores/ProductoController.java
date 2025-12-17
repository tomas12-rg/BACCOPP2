package com.baccosoft.baccosoft.Controladores;

import com.baccosoft.baccosoft.Entidades.Producto;
import com.baccosoft.baccosoft.Repositorios.ProductoRepositorio;
import com.baccosoft.baccosoft.Repositorios.DetalleVentaRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/productos")
@CrossOrigin(origins = "*")
public class ProductoController {

    @Autowired
    private ProductoRepositorio productoRepositorio;

    @Autowired
    private DetalleVentaRepositorio detalleVentaRepositorio;

    @GetMapping
    public ResponseEntity<List<Producto>> listarProductos(
            @RequestParam(required = false, defaultValue = "false") Boolean incluirInactivos) {
        
        if (incluirInactivos) {
            return ResponseEntity.ok(productoRepositorio.findAll());
        }
        return ResponseEntity.ok(productoRepositorio.findByActivoTrue());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerProducto(@PathVariable String id) {
        try {
            // ✅ VALIDAR que el ID sea numérico
            Long productId = Long.parseLong(id);
            
            return productoRepositorio.findById(productId)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
                    
        } catch (NumberFormatException e) {
            System.err.println("❌ ID inválido: " + id);
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "error", "ID_INVALIDO",
                    "mensaje", "El ID debe ser un número válido"
                ));
        }
    }

    @PostMapping
    public ResponseEntity<?> crearProducto(@RequestBody Producto producto) {
        try {
            if (producto.getNombre() == null || producto.getNombre().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "NOMBRE_REQUERIDO", "mensaje", "El nombre es obligatorio"));
            }
            
            if (producto.getPrecio() == null || producto.getPrecio() <= 0) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "PRECIO_INVALIDO", "mensaje", "El precio debe ser mayor a 0"));
            }

            // Asegurar que el producto se crea activo
            if (producto.getActivo() == null) {
                producto.setActivo(true);
            }

            Producto productoGuardado = productoRepositorio.save(producto);
            System.out.println("✅ Producto creado: " + productoGuardado.getNombre());
            
            return ResponseEntity.ok(productoGuardado);
        } catch (Exception e) {
            System.err.println("❌ Error al crear producto: " + e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", "ERROR_CREACION", "mensaje", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarProducto(@PathVariable String id, @RequestBody Producto productoActualizado) {
        try {
            // ✅ VALIDAR que el ID sea numérico
            Long productId = Long.parseLong(id);
            
            return productoRepositorio.findById(productId)
                    .map(producto -> {
                        producto.setNombre(productoActualizado.getNombre());
                        producto.setDescripcion(productoActualizado.getDescripcion());
                        producto.setPrecio(productoActualizado.getPrecio());
                        producto.setCosto(productoActualizado.getCosto());
                        producto.setStock(productoActualizado.getStock());
                        producto.setCategoria(productoActualizado.getCategoria());
                        
                        // Permitir actualizar el estado activo
                        if (productoActualizado.getActivo() != null) {
                            producto.setActivo(productoActualizado.getActivo());
                        }
                        
                        Producto productoGuardado = productoRepositorio.save(producto);
                        System.out.println("✅ Producto actualizado: " + productoGuardado.getNombre());
                        
                        return ResponseEntity.ok(productoGuardado);
                    })
                    .orElse(ResponseEntity.notFound().build());
                    
        } catch (NumberFormatException e) {
            System.err.println("❌ ID inválido: " + id);
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "error", "ID_INVALIDO",
                    "mensaje", "El ID debe ser un número válido"
                ));
        } catch (Exception e) {
            System.err.println("❌ Error al actualizar producto: " + e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", "ERROR_ACTUALIZACION", "mensaje", e.getMessage()));
        }
    }

    @PutMapping("/{id}/margen")
    public ResponseEntity<?> actualizarMargen(@PathVariable String id, @RequestBody Map<String, Double> request) {
        try {
            Long productId = Long.parseLong(id);
            Double margen = request.get("margen");
            
            if (margen == null || margen < 0) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "MARGEN_INVALIDO", "mensaje", "El margen debe ser mayor o igual a 0"));
            }
            
            return productoRepositorio.findById(productId)
                    .map(producto -> {
                        double nuevoPrecio = producto.getCosto() * (1 + margen / 100);
                        producto.setPrecio(nuevoPrecio);
                        
                        Producto productoGuardado = productoRepositorio.save(producto);
                        System.out.println("✅ Margen actualizado: " + producto.getNombre() + " - Nuevo precio: $" + nuevoPrecio);
                        
                        return ResponseEntity.ok(productoGuardado);
                    })
                    .orElse(ResponseEntity.notFound().build());
                    
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "ID_INVALIDO", "mensaje", "El ID debe ser un número válido"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "ERROR_MARGEN", "mensaje", e.getMessage()));
        }
    }

    @PutMapping("/{id}/toggle-estado")
    public ResponseEntity<?> toggleEstadoProducto(@PathVariable String id) {
        try {
            Long productId = Long.parseLong(id);
            
            return productoRepositorio.findById(productId)
                    .map(producto -> {
                        producto.setActivo(!producto.getActivo());
                        Producto productoGuardado = productoRepositorio.save(producto);
                        
                        String estado = productoGuardado.getActivo() ? "activado" : "desactivado";
                        System.out.println("✅ Producto " + estado + ": " + productoGuardado.getNombre());
                        
                        return ResponseEntity.ok(Map.of(
                            "mensaje", "Producto " + estado + " correctamente",
                            "producto", productoGuardado
                        ));
                    })
                    .orElse(ResponseEntity.notFound().build());
                    
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "ID_INVALIDO", "mensaje", "El ID debe ser un número válido"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "ERROR_TOGGLE", "mensaje", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarProducto(@PathVariable String id) {
        try {
            Long productId = Long.parseLong(id);
            
            // Verificar si el producto existe
            Producto producto = productoRepositorio.findById(productId)
                .orElse(null);
                
            if (producto == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Verificar si tiene ventas asociadas
            Long ventasCount = detalleVentaRepositorio.countByProductoId(productId);
            
            if (ventasCount > 0) {
                // Si tiene ventas, solo desactivarlo
                producto.setActivo(false);
                productoRepositorio.save(producto);
                
                System.out.println("⚠️ Producto deshabilitado (tiene " + ventasCount + " ventas): " + producto.getNombre());
                
                return ResponseEntity.ok(Map.of(
                    "mensaje", "Producto deshabilitado porque tiene ventas registradas",
                    "detalle", "El producto tiene " + ventasCount + " venta(s) asociada(s)",
                    "tipo", "deshabilitado",
                    "producto", producto
                ));
            } else {
                // Si NO tiene ventas, eliminarlo físicamente
                productoRepositorio.deleteById(productId);
                
                System.out.println("✅ Producto eliminado permanentemente: " + producto.getNombre());
                
                return ResponseEntity.ok(Map.of(
                    "mensaje", "Producto eliminado correctamente",
                    "tipo", "eliminado"
                ));
            }
            
        } catch (NumberFormatException e) {
            System.err.println("❌ ID inválido: " + id);
            return ResponseEntity.badRequest()
                .body(Map.of("error", "ID_INVALIDO", "mensaje", "El ID debe ser un número válido"));
        } catch (Exception e) {
            System.err.println("❌ Error al eliminar producto: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest()
                .body(Map.of("error", "ERROR_ELIMINACION", "mensaje", e.getMessage()));
        }
    }
}