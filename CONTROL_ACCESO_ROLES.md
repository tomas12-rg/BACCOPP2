# Control de Acceso Basado en Roles (RBAC)

## Descripción General

Este documento describe la implementación de control de acceso basado en roles en el backend de BACCOPP2. El sistema permite restringir el acceso a ciertos endpoints basándose en el rol del usuario enviado en el header HTTP.

## Roles Disponibles

El sistema reconoce tres roles de usuario definidos en `Usuario.java`:

- **ADMIN**: Acceso completo a todos los endpoints
- **SUPERVISOR**: Acceso completo (igual que ADMIN)
- **VENDEDOR**: Acceso limitado (sin acceso a reportes, estadísticas y exportaciones)

## Arquitectura de la Solución

### 1. RoleValidationInterceptor

**Ubicación**: `src/main/java/com/baccosoft/baccosoft/Configuracion/RoleValidationInterceptor.java`

Interceptor que valida el rol del usuario antes de permitir el acceso a endpoints protegidos.

**Características**:
- Lee el rol del usuario desde el header `X-User-Role`
- Valida que el rol sea ADMIN o SUPERVISOR para endpoints protegidos
- Retorna HTTP 403 (Forbidden) con mensaje JSON si no tiene permisos
- Permite requests OPTIONS para compatibilidad con CORS

### 2. WebConfig

**Ubicación**: `src/main/java/com/baccosoft/baccosoft/Configuracion/WebConfig.java`

Configuración de Spring MVC que registra el interceptor para todos los endpoints `/api/**`.

### 3. Configuración CORS

**Archivos modificados**:
- `CorsConfig.java`: Permite el header `X-User-Role` en las peticiones
- `SecurityConfig.java`: Expone el header `X-User-Role` en las respuestas

## Endpoints Protegidos

### Requieren ADMIN o SUPERVISOR

Los siguientes endpoints SOLO pueden ser accedidos por usuarios con rol ADMIN o SUPERVISOR:

#### Reportes (`/api/reportes/**`)
- `GET /api/reportes/ventas` - Obtener ventas filtradas
- `GET /api/reportes/ventas/por-dia` - Ventas agrupadas por día
- `GET /api/reportes/grafico/ventas-por-periodo` - Datos para gráfico de ventas
- `GET /api/reportes/grafico/productos-mas-vendidos` - Datos para gráfico de productos
- `GET /api/reportes/productos-mas-vendidos` - Lista de productos más vendidos
- `GET /api/reportes/ventas/por-metodo-pago` - Ventas por método de pago
- `GET /api/reportes/ventas/por-categoria` - Ventas por categoría
- `GET /api/reportes/resumen` - Resumen general de ventas
- `GET /api/reportes/exportar/productos-mas-vendidos/pdf` - Exportar a PDF
- `GET /api/reportes/exportar/productos-mas-vendidos/excel` - Exportar a Excel
- `GET /api/reportes/exportar/resumen-ventas/pdf` - Exportar resumen a PDF

#### Dashboard (`/api/dashboard/**`)
- `GET /api/dashboard/resumen` - Resumen del dashboard
- `GET /api/dashboard/ventas-por-metodo-pago` - Ventas por método de pago
- `GET /api/dashboard/productos-mas-vendidos` - Productos más vendidos
- `GET /api/dashboard/productos-stock-bajo` - Productos con stock bajo
- `GET /api/dashboard/ventas-ultimos-dias` - Ventas de últimos días

#### Exportaciones de Ventas (`/api/ventas/exportar/**`)
- `GET /api/ventas/exportar/pdf` - Exportar ventas a PDF
- `GET /api/ventas/exportar/excel` - Exportar ventas a Excel

### Accesibles para TODOS los roles (incluyendo VENDEDOR)

Los siguientes endpoints son accesibles para todos los usuarios sin restricción de rol:

#### Ventas (`/api/ventas`)
- `GET /api/ventas` - Listar ventas
- `POST /api/ventas` - Crear nueva venta
- `PUT /api/ventas/{id}` - Modificar venta
- `DELETE /api/ventas/{id}` - Eliminar venta
- `GET /api/ventas/{id}` - Obtener detalle de venta
- `GET /api/ventas/hoy` - Ventas del día

#### Productos (`/api/productos/**`)
- Todos los endpoints de productos son accesibles

#### Caja (`/api/caja/**`)
- Todos los endpoints de caja son accesibles

#### Usuarios (`/api/usuarios/**`)
- `POST /api/usuarios/login` - Login
- `POST /api/usuarios/register` - Registro
- Otros endpoints de usuarios

## Uso del Header X-User-Role

### Desde el Frontend

El frontend debe enviar el rol del usuario en cada petición HTTP:

```javascript
// Ejemplo con fetch
fetch('http://localhost:8080/api/reportes/ventas', {
  method: 'GET',
  headers: {
    'X-User-Role': 'ADMIN', // o 'VENDEDOR' o 'SUPERVISOR'
    'Content-Type': 'application/json'
  }
})
.then(response => {
  if (response.status === 403) {
    // Usuario sin permisos
    console.error('Acceso denegado');
  }
  return response.json();
})
.then(data => console.log(data));
```

```javascript
// Ejemplo con axios
axios.get('http://localhost:8080/api/reportes/ventas', {
  headers: {
    'X-User-Role': 'ADMIN'
  }
})
.catch(error => {
  if (error.response.status === 403) {
    // Usuario sin permisos
    console.error('Acceso denegado:', error.response.data);
  }
});
```

## Respuestas de Error

### Sin rol en el header

**HTTP 403 Forbidden**

```json
{
  "error": "NO_ROLE",
  "mensaje": "No se proporcionó el rol de usuario en el header",
  "header_requerido": "X-User-Role"
}
```

### Rol insuficiente (ej: VENDEDOR intentando acceder a reportes)

**HTTP 403 Forbidden**

```json
{
  "error": "ACCESO_DENEGADO",
  "mensaje": "No tienes permisos para acceder a esta sección",
  "rol_requerido": "ADMIN o SUPERVISOR",
  "tu_rol": "VENDEDOR"
}
```

## Ejemplos de Escenarios

### Escenario 1: Usuario VENDEDOR intenta acceder a reportes

```bash
# Request
curl -X GET http://localhost:8080/api/reportes/ventas \
  -H "X-User-Role: VENDEDOR"

# Response: 403 Forbidden
{
  "error": "ACCESO_DENEGADO",
  "mensaje": "No tienes permisos para acceder a esta sección",
  "rol_requerido": "ADMIN o SUPERVISOR",
  "tu_rol": "VENDEDOR"
}
```

### Escenario 2: Usuario ADMIN accede a reportes

```bash
# Request
curl -X GET http://localhost:8080/api/reportes/ventas \
  -H "X-User-Role: ADMIN"

# Response: 200 OK
[
  {
    "id": 1,
    "fecha": "2025-12-17T10:30:00",
    "total": 1500.00,
    ...
  }
]
```

### Escenario 3: Usuario VENDEDOR crea una venta (permitido)

```bash
# Request
curl -X POST http://localhost:8080/api/ventas \
  -H "X-User-Role: VENDEDOR" \
  -H "Content-Type: application/json" \
  -d '{
    "detalles": [...],
    "metodoPago": "EFECTIVO"
  }'

# Response: 200 OK
{
  "mensaje": "Venta realizada con éxito",
  "id": 123,
  "total": 1500.00
}
```

### Escenario 4: Usuario SUPERVISOR accede al dashboard

```bash
# Request
curl -X GET http://localhost:8080/api/dashboard/resumen \
  -H "X-User-Role: SUPERVISOR"

# Response: 200 OK
{
  "totalVentasHoy": 5000.00,
  "cantidadVentasHoy": 15,
  "cajaAbierta": true,
  "productosStockBajo": 3
}
```

## Testing

### Ejecutar Tests

```bash
# Ejecutar todos los tests
./mvnw test

# Ejecutar solo tests de validación de roles
./mvnw test -Dtest=RoleValidationTests
```

### Tests Implementados

El archivo `RoleValidationTests.java` contiene 13 tests que verifican:

1. Acceso a reportes sin rol → 403
2. Acceso a reportes con rol VENDEDOR → 403
3. Acceso a reportes con rol ADMIN → 200
4. Acceso a reportes con rol SUPERVISOR → 200
5. Acceso a exportación con rol VENDEDOR → 403
6. Acceso a exportación con rol ADMIN → 204 (sin datos)
7. Acceso a ventas regulares con VENDEDOR → 200
8. Acceso a ventas regulares sin rol → 200
9. Acceso a productos con VENDEDOR → 200
10. Acceso a dashboard sin rol → 403
11. Acceso a dashboard con VENDEDOR → 403
12. Acceso a dashboard con ADMIN → 200
13. Acceso a dashboard con SUPERVISOR → 200

## Logs del Sistema

El interceptor genera logs claros para debugging:

```
🔒 Validando acceso a: /api/reportes/ventas
👤 Rol del usuario: VENDEDOR
❌ Acceso denegado - Rol insuficiente: VENDEDOR
```

```
🔒 Validando acceso a: /api/reportes/ventas
👤 Rol del usuario: ADMIN
✅ Acceso permitido para rol: ADMIN
```

## Consideraciones de Seguridad

### Limitaciones Actuales

1. **Validación Simple por Header**: El rol se envía en un header HTTP sin encriptación. Esto es adecuado para desarrollo pero NO es seguro para producción.

2. **Sin Autenticación Real**: El sistema confía en el header enviado por el frontend sin verificar la identidad del usuario.

### Futuras Mejoras Recomendadas

1. **Implementar JWT (JSON Web Tokens)**:
   - El rol debería estar incluido en el token JWT
   - El token debería ser firmado por el servidor
   - Validar el token en cada petición

2. **Autenticación Stateful**:
   - Implementar sesiones del lado del servidor
   - Validar la identidad del usuario antes de confiar en el rol

3. **Auditoría**:
   - Registrar todos los intentos de acceso denegado
   - Alertar en caso de múltiples intentos fallidos

## Extensibilidad

La arquitectura actual es fácilmente extensible para:

1. **Agregar nuevos roles**: Modificar el enum `Rol` en `Usuario.java`
2. **Proteger nuevos endpoints**: Agregar condiciones en el método `requiresAdminOrSupervisor()`
3. **Roles granulares**: Implementar diferentes niveles de acceso para diferentes endpoints
4. **Migración a JWT**: El interceptor puede ser modificado para leer el rol desde un JWT en lugar del header

## Mantenimiento

### Agregar Protección a Nuevos Endpoints

Para proteger un nuevo endpoint, editar el método `requiresAdminOrSupervisor()` en `RoleValidationInterceptor.java`:

```java
private boolean requiresAdminOrSupervisor(String uri) {
    if (uri.startsWith("/api/reportes/")) return true;
    if (uri.startsWith("/api/dashboard/")) return true;
    if (uri.startsWith("/api/ventas/exportar/")) return true;
    if (uri.startsWith("/api/ventas/estadisticas/")) return true;
    
    // Agregar nuevos endpoints aquí
    if (uri.startsWith("/api/nuevo-endpoint/")) return true;
    
    return false;
}
```

### Modificar Mensaje de Error

Para personalizar los mensajes de error, editar el método `sendForbiddenResponse()` en `RoleValidationInterceptor.java`.

## Compatibilidad

- **Spring Boot**: 3.1.5
- **Java**: 17
- **Base de datos**: Compatible con PostgreSQL (producción) y H2 (tests)

## Soporte

Para preguntas o problemas con el control de acceso basado en roles, revisar:

1. Logs del servidor para mensajes del interceptor
2. Tests en `RoleValidationTests.java` para ejemplos de uso
3. Respuesta JSON de error que indica el problema específico
