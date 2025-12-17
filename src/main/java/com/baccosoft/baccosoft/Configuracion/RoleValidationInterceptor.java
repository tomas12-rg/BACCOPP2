package com.baccosoft.baccosoft.Configuracion;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.HashMap;
import java.util.Map;

@Component
public class RoleValidationInterceptor implements HandlerInterceptor {

    private static final String ROLE_HEADER = "X-User-Role";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();
        
        // Permitir preflight requests (OPTIONS)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // Endpoints que requieren ADMIN o SUPERVISOR
        if (requiresAdminOrSupervisor(requestURI)) {
            String userRole = request.getHeader(ROLE_HEADER);
            
            System.out.println("🔒 Validando acceso a: " + requestURI);
            System.out.println("👤 Rol del usuario: " + userRole);

            // Si no hay rol en el header, denegar acceso
            if (userRole == null || userRole.trim().isEmpty()) {
                System.err.println("❌ Acceso denegado - No se proporcionó rol");
                sendForbiddenResponse(response, "NO_ROLE", null);
                return false;
            }

            // Validar que el rol sea ADMIN o SUPERVISOR
            if (!isAdminOrSupervisor(userRole)) {
                System.err.println("❌ Acceso denegado - Rol insuficiente: " + userRole);
                sendForbiddenResponse(response, "ACCESO_DENEGADO", userRole);
                return false;
            }

            System.out.println("✅ Acceso permitido para rol: " + userRole);
        }

        return true;
    }

    /**
     * Verifica si el endpoint requiere permisos de ADMIN o SUPERVISOR
     */
    private boolean requiresAdminOrSupervisor(String uri) {
        // Endpoints de reportes - TODOS requieren ADMIN/SUPERVISOR
        if (uri.startsWith("/api/reportes/")) {
            return true;
        }

        // Endpoints de dashboard - Estadísticas requieren ADMIN/SUPERVISOR
        if (uri.startsWith("/api/dashboard/")) {
            return true;
        }

        // Endpoints de exportación de ventas - requieren ADMIN/SUPERVISOR
        if (uri.startsWith("/api/ventas/exportar/")) {
            return true;
        }

        // Endpoints de estadísticas (si existen en el futuro)
        if (uri.startsWith("/api/ventas/estadisticas/")) {
            return true;
        }

        return false;
    }

    /**
     * Verifica si el rol es ADMIN o SUPERVISOR
     */
    private boolean isAdminOrSupervisor(String role) {
        return "ADMIN".equalsIgnoreCase(role) || "SUPERVISOR".equalsIgnoreCase(role);
    }

    /**
     * Envía respuesta de error 403 Forbidden con formato JSON
     */
    private void sendForbiddenResponse(HttpServletResponse response, String errorCode, String userRole) throws Exception {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", errorCode);
        
        if ("NO_ROLE".equals(errorCode)) {
            errorResponse.put("mensaje", "No se proporcionó el rol de usuario en el header");
            errorResponse.put("header_requerido", ROLE_HEADER);
        } else {
            errorResponse.put("mensaje", "No tienes permisos para acceder a esta sección");
            errorResponse.put("rol_requerido", "ADMIN o SUPERVISOR");
            errorResponse.put("tu_rol", userRole);
        }

        String jsonResponse = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }
}
