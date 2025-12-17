package com.baccosoft.baccosoft.Servicios;

import com.baccosoft.baccosoft.Entidades.Venta;
import com.baccosoft.baccosoft.Entidades.DetalleVenta;
import com.itextpdf.text.*;
import com.itextpdf.text.Font;
import com.itextpdf.text.pdf.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExportacionServicio {

    private static final BaseColor COLOR_PRIMARIO = new BaseColor(128, 0, 32);
    private static final BaseColor COLOR_SECUNDARIO = new BaseColor(96, 0, 24);
    private static final BaseColor COLOR_FONDO_HEADER = new BaseColor(240, 240, 240);
    private static final BaseColor COLOR_TEXTO_CLARO = BaseColor.WHITE;
    
    private static final NumberFormat MONEY_FORMAT = NumberFormat.getCurrencyInstance(new Locale("es", "AR"));

    // ✅ DTO para agrupar productos
    private static class ProductoVendido {
        String nombre;
        String categoria;
        int cantidadVendida;
        double precioUnitario;
        double totalVendido;

        public ProductoVendido(String nombre, String categoria, int cantidad, double precio) {
            this.nombre = nombre;
            this.categoria = categoria;
            this.cantidadVendida = cantidad;
            this.precioUnitario = precio;
            this.totalVendido = cantidad * precio;
        }
    }

    /**
     * ✅ EXPORTA REPORTE DE PRODUCTOS MÁS VENDIDOS A PDF
     */
    public byte[] exportarProductosMasVendidosPDF(java.util.List<Venta> ventas) throws Exception {
        Document documento = new Document(PageSize.A4.rotate());
        documento.setMargins(40, 40, 60, 60);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            PdfWriter writer = PdfWriter.getInstance(documento, baos);
            writer.setCompressionLevel(0);
            writer.setPdfVersion(PdfWriter.VERSION_1_4);
            
            documento.open();

            // Logo
            try {
                ClassPathResource imgFile = new ClassPathResource("static/images/logo.png");
                Image logo = Image.getInstance(imgFile.getURL());
                logo.scaleToFit(100, 100);
                logo.setAlignment(Element.ALIGN_CENTER);
                documento.add(logo);
                documento.add(new Paragraph("\n"));
            } catch (Exception e) {
                System.err.println("⚠️ No se pudo cargar el logo: " + e.getMessage());
            }

            // Encabezado
            Font tituloFont = new Font(Font.FontFamily.HELVETICA, 24, Font.BOLD, COLOR_PRIMARIO);
            Paragraph titulo = new Paragraph("Reporte de Productos Más Vendidos", tituloFont);
            titulo.setAlignment(Element.ALIGN_CENTER);
            documento.add(titulo);

            String fechaGeneracion = java.time.LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
            Font subtituloFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL, BaseColor.DARK_GRAY);
            Paragraph subtitulo = new Paragraph("Generado el: " + fechaGeneracion, subtituloFont);
            subtitulo.setAlignment(Element.ALIGN_CENTER);
            documento.add(subtitulo);
            documento.add(new Paragraph("\n"));

            // Agrupar productos
            Map<String, ProductoVendido> productosMap = new HashMap<>();

            for (Venta venta : ventas) {
                if (venta.getDetalles() != null) {
                    for (DetalleVenta detalle : venta.getDetalles()) {
                        String key = detalle.getProducto().getId().toString();
                        
                        if (productosMap.containsKey(key)) {
                            ProductoVendido pv = productosMap.get(key);
                            pv.cantidadVendida += detalle.getCantidad();
                            pv.totalVendido = pv.cantidadVendida * pv.precioUnitario;
                        } else {
                            productosMap.put(key, new ProductoVendido(
                                detalle.getProducto().getNombre(),
                                detalle.getProducto().getCategoria(),
                                detalle.getCantidad(),
                                detalle.getPrecioUnitario()
                            ));
                        }
                    }
                }
            }

            java.util.List<ProductoVendido> productosOrdenados = productosMap.values().stream()
                .sorted((a, b) -> Integer.compare(b.cantidadVendida, a.cantidadVendida))
                .collect(Collectors.toList());

            // Tabla de productos
            PdfPTable tabla = new PdfPTable(5);
            tabla.setWidthPercentage(100);
            tabla.setWidths(new float[]{4, 2, 1.5f, 2, 2});

            Font headerFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, COLOR_TEXTO_CLARO);
            Font cellFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.BLACK);

            String[] headers = {"Producto", "Categoría", "Cantidad", "Precio Unit.", "Total Vendido"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setBackgroundColor(COLOR_PRIMARIO);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setPadding(10);
                cell.setBorderColor(COLOR_SECUNDARIO);
                tabla.addCell(cell);
            }

            double totalGeneral = 0;
            int cantidadTotal = 0;

            for (int i = 0; i < productosOrdenados.size(); i++) {
                ProductoVendido pv = productosOrdenados.get(i);
                BaseColor bgColor = (i % 2 == 0) ? BaseColor.WHITE : COLOR_FONDO_HEADER;

                agregarCelda(tabla, pv.nombre, cellFont, bgColor, Element.ALIGN_LEFT);
                agregarCelda(tabla, pv.categoria, cellFont, bgColor, Element.ALIGN_CENTER);
                agregarCelda(tabla, String.valueOf(pv.cantidadVendida), cellFont, bgColor, Element.ALIGN_CENTER);
                agregarCelda(tabla, MONEY_FORMAT.format(pv.precioUnitario), cellFont, bgColor, Element.ALIGN_RIGHT);
                
                Font totalFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, COLOR_PRIMARIO);
                agregarCelda(tabla, MONEY_FORMAT.format(pv.totalVendido), totalFont, bgColor, Element.ALIGN_RIGHT);

                totalGeneral += pv.totalVendido;
                cantidadTotal += pv.cantidadVendida;
            }

            documento.add(tabla);

            // Resumen
            documento.add(new Paragraph("\n"));
            Font resumenFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, COLOR_PRIMARIO);
            
            Paragraph resumen1 = new Paragraph("CANTIDAD TOTAL VENDIDA: " + cantidadTotal + " unidades", resumenFont);
            resumen1.setAlignment(Element.ALIGN_RIGHT);
            documento.add(resumen1);

            Paragraph resumen2 = new Paragraph("TOTAL GENERAL: " + MONEY_FORMAT.format(totalGeneral), resumenFont);
            resumen2.setAlignment(Element.ALIGN_RIGHT);
            documento.add(resumen2);

            Font infoFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.DARK_GRAY);
            Paragraph info = new Paragraph("\nDocumento generado automáticamente por BaccoSoft", infoFont);
            info.setAlignment(Element.ALIGN_CENTER);
            documento.add(info);

        } finally {
            documento.close();
        }

        return baos.toByteArray();
    }

    /**
     * ✅ EXPORTA REPORTE DE PRODUCTOS MÁS VENDIDOS A EXCEL
     */
    public byte[] exportarProductosMasVendidosExcel(java.util.List<Venta> ventas) throws Exception {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Productos Más Vendidos");

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.DARK_RED.index);
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setColor(IndexedColors.WHITE.index);
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            CellStyle moneyStyle = workbook.createCellStyle();
            DataFormat format = workbook.createDataFormat();
            moneyStyle.setDataFormat(format.getFormat("$#,##0.00"));

            Map<String, ProductoVendido> productosMap = new HashMap<>();

            for (Venta venta : ventas) {
                if (venta.getDetalles() != null) {
                    for (DetalleVenta detalle : venta.getDetalles()) {
                        String key = detalle.getProducto().getId().toString();
                        
                        if (productosMap.containsKey(key)) {
                            ProductoVendido pv = productosMap.get(key);
                            pv.cantidadVendida += detalle.getCantidad();
                            pv.totalVendido = pv.cantidadVendida * pv.precioUnitario;
                        } else {
                            productosMap.put(key, new ProductoVendido(
                                detalle.getProducto().getNombre(),
                                detalle.getProducto().getCategoria(),
                                detalle.getCantidad(),
                                detalle.getPrecioUnitario()
                            ));
                        }
                    }
                }
            }

            java.util.List<ProductoVendido> productosOrdenados = productosMap.values().stream()
                .sorted((a, b) -> Integer.compare(b.cantidadVendida, a.cantidadVendida))
                .collect(Collectors.toList());

            Row headerRow = sheet.createRow(0);
            String[] columns = {"Producto", "Categoría", "Cantidad", "Precio Unitario", "Total Vendido"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (ProductoVendido pv : productosOrdenados) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(pv.nombre);
                row.createCell(1).setCellValue(pv.categoria);
                row.createCell(2).setCellValue(pv.cantidadVendida);
                
                Cell precioCell = row.createCell(3);
                precioCell.setCellValue(pv.precioUnitario);
                precioCell.setCellStyle(moneyStyle);

                Cell totalCell = row.createCell(4);
                totalCell.setCellValue(pv.totalVendido);
                totalCell.setCellStyle(moneyStyle);
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    /**
     * ✅ NUEVO: RESUMEN SIMPLE (para pantalla "Reportes")
     * Tabla con: ID | Fecha | Método Pago | # Productos | Total
     */
    public byte[] exportarResumenVentasPDF(java.util.List<Venta> ventas) throws Exception {
        Document documento = new Document(PageSize.A4);
        documento.setMargins(40, 40, 60, 60);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            PdfWriter writer = PdfWriter.getInstance(documento, baos);
            writer.setCompressionLevel(0);
            writer.setPdfVersion(PdfWriter.VERSION_1_4);
            
            documento.open();

            // Logo
            try {
                ClassPathResource imgFile = new ClassPathResource("static/images/logo.png");
                Image logo = Image.getInstance(imgFile.getURL());
                logo.scaleToFit(100, 100);
                logo.setAlignment(Element.ALIGN_CENTER);
                documento.add(logo);
                documento.add(new Paragraph("\n"));
            } catch (Exception e) {
                System.err.println("⚠️ No se pudo cargar el logo: " + e.getMessage());
            }

            // Encabezado
            Font tituloFont = new Font(Font.FontFamily.HELVETICA, 24, Font.BOLD, COLOR_PRIMARIO);
            Paragraph titulo = new Paragraph("BaccoSoft - Resumen de Ventas", tituloFont);
            titulo.setAlignment(Element.ALIGN_CENTER);
            documento.add(titulo);

            String fechaGeneracion = java.time.LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
            Font subtituloFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL, BaseColor.DARK_GRAY);
            Paragraph subtitulo = new Paragraph("Generado el: " + fechaGeneracion, subtituloFont);
            subtitulo.setAlignment(Element.ALIGN_CENTER);
            documento.add(subtitulo);

            Paragraph total = new Paragraph("Total de ventas: " + ventas.size(), subtituloFont);
            total.setAlignment(Element.ALIGN_CENTER);
            documento.add(total);
            documento.add(new Paragraph("\n"));

            // Tabla resumen
            PdfPTable tabla = new PdfPTable(5);
            tabla.setWidthPercentage(100);
            tabla.setWidths(new float[]{1, 3, 2.5f, 2, 2});

            Font headerFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, COLOR_TEXTO_CLARO);
            Font cellFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.BLACK);

            String[] headers = {"ID", "Fecha y Hora", "Método de Pago", "Productos", "Total"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setBackgroundColor(COLOR_PRIMARIO);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setPadding(10);
                cell.setBorderColor(COLOR_SECUNDARIO);
                tabla.addCell(cell);
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            
            for (int i = 0; i < ventas.size(); i++) {
                Venta venta = ventas.get(i);
                BaseColor bgColor = (i % 2 == 0) ? BaseColor.WHITE : COLOR_FONDO_HEADER;

                agregarCelda(tabla, String.valueOf(venta.getId()), cellFont, bgColor, Element.ALIGN_CENTER);
                agregarCelda(tabla, venta.getFecha().format(formatter), cellFont, bgColor, Element.ALIGN_LEFT);
                agregarCelda(tabla, formatearMetodoPago(venta.getMetodoPago()), cellFont, bgColor, Element.ALIGN_CENTER);
                
                int cantidadProductos = venta.getDetalles() != null ? venta.getDetalles().size() : 0;
                agregarCelda(tabla, cantidadProductos + " item(s)", cellFont, bgColor, Element.ALIGN_CENTER);

                Font totalFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, COLOR_PRIMARIO);
                agregarCelda(tabla, MONEY_FORMAT.format(venta.getTotal()), totalFont, bgColor, Element.ALIGN_RIGHT);
            }

            documento.add(tabla);

            // Total general
            documento.add(new Paragraph("\n"));
            double totalGeneral = ventas.stream().mapToDouble(Venta::getTotal).sum();
            Font resumenFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, COLOR_PRIMARIO);
            
            Paragraph resumen = new Paragraph(
                "TOTAL GENERAL: " + MONEY_FORMAT.format(totalGeneral),
                resumenFont
            );
            resumen.setAlignment(Element.ALIGN_RIGHT);
            documento.add(resumen);

            Font infoFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.DARK_GRAY);
            Paragraph info = new Paragraph("\nDocumento generado automáticamente por BaccoSoft", infoFont);
            info.setAlignment(Element.ALIGN_CENTER);
            documento.add(info);

        } finally {
            documento.close();
        }

        return baos.toByteArray();
    }

    /**
     * ✅ DETALLADO: Exporta cada venta con sus productos (para pantalla "Ventas")
     */
    public byte[] exportarVentasPDF(java.util.List<Venta> ventas) throws Exception {
        Document documento = new Document(PageSize.A4);
        documento.setMargins(40, 40, 60, 60);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            PdfWriter writer = PdfWriter.getInstance(documento, baos);
            writer.setCompressionLevel(0);
            writer.setPdfVersion(PdfWriter.VERSION_1_4);
            
            documento.open();

            // Logo
            try {
                ClassPathResource imgFile = new ClassPathResource("static/images/logo.png");
                Image logo = Image.getInstance(imgFile.getURL());
                logo.scaleToFit(100, 100);
                logo.setAlignment(Element.ALIGN_CENTER);
                documento.add(logo);
                documento.add(new Paragraph("\n"));
            } catch (Exception e) {
                System.err.println("⚠️ No se pudo cargar el logo: " + e.getMessage());
            }

            // Encabezado
            Font tituloFont = new Font(Font.FontFamily.HELVETICA, 24, Font.BOLD, COLOR_PRIMARIO);
            Paragraph titulo = new Paragraph("BaccoSoft - Reporte Detallado de Ventas", tituloFont);
            titulo.setAlignment(Element.ALIGN_CENTER);
            documento.add(titulo);

            String fechaGeneracion = java.time.LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
            Font subtituloFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL, BaseColor.DARK_GRAY);
            Paragraph subtitulo = new Paragraph("Generado el: " + fechaGeneracion, subtituloFont);
            subtitulo.setAlignment(Element.ALIGN_CENTER);
            documento.add(subtitulo);

            Paragraph total = new Paragraph("Total de ventas: " + ventas.size(), subtituloFont);
            total.setAlignment(Element.ALIGN_CENTER);
            documento.add(total);
            documento.add(new Paragraph("\n"));

            // Resumen general
            double totalGeneral = ventas.stream().mapToDouble(Venta::getTotal).sum();
            
            PdfPTable resumenTable = new PdfPTable(2);
            resumenTable.setWidthPercentage(60);
            resumenTable.setHorizontalAlignment(Element.ALIGN_CENTER);
            
            Font headerFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, COLOR_TEXTO_CLARO);
            
            PdfPCell cell1 = new PdfPCell(new Phrase("Total Ventas", headerFont));
            cell1.setBackgroundColor(COLOR_PRIMARIO);
            cell1.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell1.setPadding(10);
            resumenTable.addCell(cell1);
            
            PdfPCell cell2 = new PdfPCell(new Phrase("Cantidad", headerFont));
            cell2.setBackgroundColor(COLOR_PRIMARIO);
            cell2.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell2.setPadding(10);
            resumenTable.addCell(cell2);
            
            Font resumenFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, COLOR_PRIMARIO);
            
            PdfPCell cell3 = new PdfPCell(new Phrase(MONEY_FORMAT.format(totalGeneral), resumenFont));
            cell3.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell3.setPadding(10);
            resumenTable.addCell(cell3);
            
            PdfPCell cell4 = new PdfPCell(new Phrase(String.valueOf(ventas.size()), resumenFont));
            cell4.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell4.setPadding(10);
            resumenTable.addCell(cell4);
            
            documento.add(resumenTable);
            documento.add(new Paragraph("\n\n"));

            // DETALLE DE CADA VENTA CON SUS PRODUCTOS
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            Font ventaHeaderFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, COLOR_PRIMARIO);
            Font cellFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.BLACK);
            Font infoFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.DARK_GRAY);

            for (int i = 0; i < ventas.size(); i++) {
                Venta venta = ventas.get(i);

                // Encabezado de venta
                Paragraph ventaHeader = new Paragraph();
                ventaHeader.add(new Chunk("Venta #" + venta.getId(), ventaHeaderFont));
                ventaHeader.add(new Chunk("  |  ", infoFont));
                ventaHeader.add(new Chunk(venta.getFecha().format(formatter), infoFont));
                documento.add(ventaHeader);

                // Info de venta
                Paragraph ventaInfo = new Paragraph();
                Font boldFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.BLACK);
                ventaInfo.add(new Chunk("Método: ", boldFont));
                ventaInfo.add(new Chunk(formatearMetodoPago(venta.getMetodoPago()), cellFont));
                ventaInfo.add(new Chunk("  |  ", infoFont));
                ventaInfo.add(new Chunk("Total: ", boldFont));
                ventaInfo.add(new Chunk(MONEY_FORMAT.format(venta.getTotal()), ventaHeaderFont));
                documento.add(ventaInfo);
                documento.add(new Paragraph(" "));

                // TABLA DE PRODUCTOS
                if (venta.getDetalles() != null && !venta.getDetalles().isEmpty()) {
                    PdfPTable productosTable = new PdfPTable(4);
                    productosTable.setWidthPercentage(100);
                    productosTable.setWidths(new float[]{4, 1, 1.5f, 1.5f});

                    Font productoHeaderFont = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, COLOR_TEXTO_CLARO);
                    
                    String[] headers = {"Producto", "Cant.", "Precio Unit.", "Subtotal"};
                    for (String header : headers) {
                        PdfPCell headerCell = new PdfPCell(new Phrase(header, productoHeaderFont));
                        headerCell.setBackgroundColor(new BaseColor(180, 180, 180));
                        headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                        headerCell.setPadding(6);
                        productosTable.addCell(headerCell);
                    }

                    Font productoFont = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, BaseColor.BLACK);
                    
                    for (DetalleVenta detalle : venta.getDetalles()) {
                        String nombreProducto = detalle.getProducto() != null 
                            ? detalle.getProducto().getNombre() 
                            : "Producto N/A";

                        PdfPCell c1 = new PdfPCell(new Phrase(nombreProducto, productoFont));
                        c1.setHorizontalAlignment(Element.ALIGN_LEFT);
                        c1.setPadding(5);
                        productosTable.addCell(c1);

                        PdfPCell c2 = new PdfPCell(new Phrase(String.valueOf(detalle.getCantidad()), productoFont));
                        c2.setHorizontalAlignment(Element.ALIGN_CENTER);
                        c2.setPadding(5);
                        productosTable.addCell(c2);

                        PdfPCell c3 = new PdfPCell(new Phrase(MONEY_FORMAT.format(detalle.getPrecioUnitario()), productoFont));
                        c3.setHorizontalAlignment(Element.ALIGN_RIGHT);
                        c3.setPadding(5);
                        productosTable.addCell(c3);

                        Font subtotalFont = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, new BaseColor(46, 125, 50));
                        PdfPCell c4 = new PdfPCell(new Phrase(MONEY_FORMAT.format(detalle.getSubtotal()), subtotalFont));
                        c4.setHorizontalAlignment(Element.ALIGN_RIGHT);
                        c4.setPadding(5);
                        productosTable.addCell(c4);
                    }

                    documento.add(productosTable);
                }

                // Separador
                if (i < ventas.size() - 1) {
                    documento.add(new Paragraph("\n"));
                    Paragraph linea = new Paragraph("_____________________________________________________________________________");
                    linea.getFont().setColor(BaseColor.LIGHT_GRAY);
                    linea.setAlignment(Element.ALIGN_CENTER);
                    documento.add(linea);
                    documento.add(new Paragraph("\n"));
                }
            }

            // Pie de página
            documento.add(new Paragraph("\n\n"));
            Font footerFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.DARK_GRAY);
            Paragraph footer = new Paragraph("Documento generado automáticamente por BaccoSoft", footerFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            documento.add(footer);

        } finally {
            documento.close();
        }

        return baos.toByteArray();
    }

    /**
     * ✅ Exporta las ventas a Excel con 2 hojas (Resumen + Detalle)
     */
    public byte[] exportarVentasExcel(java.util.List<Venta> ventas) throws Exception {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            // HOJA 1: RESUMEN DE VENTAS
            Sheet resumenSheet = workbook.createSheet("Resumen");

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.DARK_RED.index);
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setColor(IndexedColors.WHITE.index);
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            CellStyle moneyStyle = workbook.createCellStyle();
            DataFormat format = workbook.createDataFormat();
            moneyStyle.setDataFormat(format.getFormat("$#,##0.00"));

            int rowNum = 0;
            
            // Título
            Row titleRow = resumenSheet.createRow(rowNum++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("REPORTE DE VENTAS");
            CellStyle titleStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);
            
            rowNum++;

            // Encabezados
            Row headerRow = resumenSheet.createRow(rowNum++);
            String[] headers = {"ID", "Fecha", "Método Pago", "Productos", "Total"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            
            for (Venta venta : ventas) {
                Row row = resumenSheet.createRow(rowNum++);
                row.createCell(0).setCellValue(venta.getId());
                row.createCell(1).setCellValue(venta.getFecha().format(formatter));
                row.createCell(2).setCellValue(formatearMetodoPago(venta.getMetodoPago()));
                
                // LISTA DE PRODUCTOS
                StringBuilder productos = new StringBuilder();
                if (venta.getDetalles() != null) {
                    for (int i = 0; i < venta.getDetalles().size(); i++) {
                        DetalleVenta detalle = venta.getDetalles().get(i);
                        if (i > 0) productos.append(", ");
                        productos.append(detalle.getProducto().getNombre())
                                .append(" (x").append(detalle.getCantidad()).append(")");
                    }
                }
                row.createCell(3).setCellValue(productos.toString());
                
                Cell totalCell = row.createCell(4);
                totalCell.setCellValue(venta.getTotal());
                totalCell.setCellStyle(moneyStyle);
            }

            for (int i = 0; i < headers.length; i++) {
                resumenSheet.autoSizeColumn(i);
            }

            // HOJA 2: DETALLE DE PRODUCTOS
            Sheet detalleSheet = workbook.createSheet("Detalle Productos");
            rowNum = 0;
            
            Row detalleTitleRow = detalleSheet.createRow(rowNum++);
            Cell detalleTitleCell = detalleTitleRow.createCell(0);
            detalleTitleCell.setCellValue("DETALLE DE PRODUCTOS VENDIDOS");
            detalleTitleCell.setCellStyle(titleStyle);
            
            rowNum++;
            
            Row detalleHeaderRow = detalleSheet.createRow(rowNum++);
            String[] detalleHeaders = {"ID Venta", "Fecha", "Producto", "Cantidad", "Precio Unit.", "Subtotal"};
            
            for (int i = 0; i < detalleHeaders.length; i++) {
                Cell cell = detalleHeaderRow.createCell(i);
                cell.setCellValue(detalleHeaders[i]);
                cell.setCellStyle(headerStyle);
            }

            for (Venta venta : ventas) {
                if (venta.getDetalles() != null) {
                    for (DetalleVenta detalle : venta.getDetalles()) {
                        Row row = detalleSheet.createRow(rowNum++);
                        row.createCell(0).setCellValue(venta.getId());
                        row.createCell(1).setCellValue(venta.getFecha().format(formatter));
                        row.createCell(2).setCellValue(detalle.getProducto().getNombre());
                        row.createCell(3).setCellValue(detalle.getCantidad());
                        
                        Cell precioCell = row.createCell(4);
                        precioCell.setCellValue(detalle.getPrecioUnitario());
                        precioCell.setCellStyle(moneyStyle);

                        Cell subtotalCell = row.createCell(5);
                        subtotalCell.setCellValue(detalle.getSubtotal());
                        subtotalCell.setCellStyle(moneyStyle);
                    }
                }
            }

            for (int i = 0; i < detalleHeaders.length; i++) {
                detalleSheet.autoSizeColumn(i);
            }

            workbook.write(baos);
            return baos.toByteArray();
        }
    }

    // MÉTODOS AUXILIARES
    private void agregarCelda(PdfPTable tabla, String texto, Font font, BaseColor bgColor, int alineacion) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, font));
        cell.setBackgroundColor(bgColor);
        cell.setHorizontalAlignment(alineacion);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(8);
        cell.setBorderColor(BaseColor.LIGHT_GRAY);
        tabla.addCell(cell);
    }

    private String formatearMetodoPago(String metodoPago) {
        if (metodoPago == null) return "N/A";
        
        switch (metodoPago.toUpperCase()) {
            case "EFECTIVO": return "Efectivo";
            case "TARJETA_CREDITO": return "T. Crédito";
            case "TARJETA_DEBITO": return "T. Débito";
            case "TRANSFERENCIA": return "Transferencia";
            case "MERCADO_PAGO": return "Mercado Pago";
            default: return metodoPago;
        }
    }
}