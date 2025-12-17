package com.baccosoft.baccosoft.Servicios;

import com.baccosoft.baccosoft.Enums.MetodoPago;
import org.springframework.stereotype.Service;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

@Service
public class PagoServicio {
    private final Random random = new Random();

    public CompletableFuture<ResultadoPago> procesarPago(Double monto, MetodoPago metodoPago) {
        return CompletableFuture.supplyAsync(() -> {
            // Simular delay de procesamiento
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Simular respuesta del procesador de pagos
            boolean exitoso = random.nextDouble() > 0.1; // 90% de éxito
            
            if (!exitoso) {
                return new ResultadoPago(false, "Transacción rechazada por el banco");
            }

            String numeroTransaccion = generarNumeroTransaccion();
            return new ResultadoPago(true, "Transacción exitosa", numeroTransaccion);
        });
    }

    private String generarNumeroTransaccion() {
        return String.format("%d%06d", 
            System.currentTimeMillis() / 1000, 
            random.nextInt(1000000));
    }

    public static class ResultadoPago {
        private final boolean exitoso;
        private final String mensaje;
        private final String numeroTransaccion;

        public ResultadoPago(boolean exitoso, String mensaje) {
            this(exitoso, mensaje, null);
        }

        public ResultadoPago(boolean exitoso, String mensaje, String numeroTransaccion) {
            this.exitoso = exitoso;
            this.mensaje = mensaje;
            this.numeroTransaccion = numeroTransaccion;
        }

        public boolean isExitoso() { return exitoso; }
        public String getMensaje() { return mensaje; }
        public String getNumeroTransaccion() { return numeroTransaccion; }
    }
}