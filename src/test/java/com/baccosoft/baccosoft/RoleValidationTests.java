package com.baccosoft.baccosoft;

import com.baccosoft.baccosoft.Configuracion.RoleValidationInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class RoleValidationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testReporteEndpoint_WithoutRole_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/reportes/ventas"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("NO_ROLE"));
    }

    @Test
    void testReporteEndpoint_WithVendedorRole_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/reportes/ventas")
                        .header("X-User-Role", "VENDEDOR"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCESO_DENEGADO"))
                .andExpect(jsonPath("$.tu_rol").value("VENDEDOR"));
    }

    @Test
    void testReporteEndpoint_WithAdminRole_ShouldPass() throws Exception {
        mockMvc.perform(get("/api/reportes/ventas")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk());
    }

    @Test
    void testReporteEndpoint_WithSupervisorRole_ShouldPass() throws Exception {
        mockMvc.perform(get("/api/reportes/ventas")
                        .header("X-User-Role", "SUPERVISOR"))
                .andExpect(status().isOk());
    }

    @Test
    void testExportEndpoint_WithVendedorRole_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/ventas/exportar/pdf")
                        .header("X-User-Role", "VENDEDOR"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCESO_DENEGADO"));
    }

    @Test
    void testExportEndpoint_WithAdminRole_ShouldPass() throws Exception {
        // Export endpoints return 204 (No Content) when there's no data to export
        mockMvc.perform(get("/api/ventas/exportar/pdf")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isNoContent());
    }

    @Test
    void testRegularVentaEndpoint_WithVendedorRole_ShouldPass() throws Exception {
        mockMvc.perform(get("/api/ventas")
                        .header("X-User-Role", "VENDEDOR"))
                .andExpect(status().isOk());
    }

    @Test
    void testRegularVentaEndpoint_WithoutRole_ShouldPass() throws Exception {
        // Los endpoints regulares de ventas deben ser accesibles sin rol
        mockMvc.perform(get("/api/ventas"))
                .andExpect(status().isOk());
    }

    @Test
    void testProductoEndpoint_WithVendedorRole_ShouldPass() throws Exception {
        mockMvc.perform(get("/api/productos")
                        .header("X-User-Role", "VENDEDOR"))
                .andExpect(status().isOk());
    }

    @Test
    void testDashboardEndpoint_WithoutRole_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/dashboard/resumen"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("NO_ROLE"));
    }

    @Test
    void testDashboardEndpoint_WithVendedorRole_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/dashboard/resumen")
                        .header("X-User-Role", "VENDEDOR"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCESO_DENEGADO"));
    }

    @Test
    void testDashboardEndpoint_WithAdminRole_ShouldPass() throws Exception {
        mockMvc.perform(get("/api/dashboard/resumen")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk());
    }

    @Test
    void testDashboardEndpoint_WithSupervisorRole_ShouldPass() throws Exception {
        mockMvc.perform(get("/api/dashboard/resumen")
                        .header("X-User-Role", "SUPERVISOR"))
                .andExpect(status().isOk());
    }
}
