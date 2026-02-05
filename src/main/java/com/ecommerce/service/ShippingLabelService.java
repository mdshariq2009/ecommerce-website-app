package com.ecommerce.service;

import com.ecommerce.model.Order;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;
import com.google.zxing.qrcode.QRCodeWriter;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Service
public class ShippingLabelService {

    public byte[] generateReturnShippingLabel(Order order) throws Exception {
        System.out.println("📦 Generating USPS-style return shipping label for Order #" + order.getId());
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf, new PageSize(612, 792)); // 8.5 x 11 inches
        
        try {
            document.setMargins(15, 15, 15, 15);
            
            // ========== TOP SECTION - POSTAGE PAID HEADER ==========
            Table topSection = new Table(new float[]{120, 350, 150});
            topSection.setWidth(UnitValue.createPercentValue(100));
            topSection.setBorder(new SolidBorder(ColorConstants.BLACK, 3));
            topSection.setMarginBottom(0);
            
            // Large "G" logo cell
            Cell logoCell = new Cell();
            logoCell.add(new Paragraph("G")
                .setFontSize(100)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(10))
                .setPadding(0)
                .setHeight(180)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBorderRight(new SolidBorder(ColorConstants.BLACK, 2))
                .setBorder(new SolidBorder(0));
            topSection.addCell(logoCell);
            
            // Middle section - Postage info and barcode
            Cell middleCell = new Cell();
            middleCell.add(new Paragraph("US POSTAGE & FEES PAID IMI")
                .setFontSize(13)
                .setBold()
                .setMarginBottom(2));
            middleCell.add(new Paragraph("4 OZ GROUND ADVANTAGE RATE")
                .setFontSize(11)
                .setBold()
                .setMarginBottom(2));
            middleCell.add(new Paragraph("ZONE 7")
                .setFontSize(11)
                .setBold()
                .setMarginBottom(2));
            middleCell.add(new Paragraph("Commercial")
                .setFontSize(11)
                .setBold()
                .setMarginBottom(5));
            
            // Generate horizontal barcode for top section
            String topBarcode = String.format("%020d", order.getId() * 1000000L + System.currentTimeMillis() % 1000000L);
            byte[] topBarcodeImg = generateBarcode(topBarcode, 320, 45);
            Image topBarcodeImage = new Image(ImageDataFactory.create(topBarcodeImg));
            topBarcodeImage.setWidth(320);
            topBarcodeImage.setHeight(45);
            middleCell.add(topBarcodeImage.setMarginTop(3));
            
            middleCell.setPadding(10)
                .setHeight(180)
                .setVerticalAlignment(VerticalAlignment.TOP)
                .setBorderRight(new SolidBorder(ColorConstants.BLACK, 2))
                .setBorder(new SolidBorder(0));
            topSection.addCell(middleCell);
            
            // Right section - Stamps.com info
            Cell rightCell = new Cell();
            rightCell.add(new Paragraph("⚙ Stamps.com")
                .setFontSize(11)
                .setBold()
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginBottom(5));
            
            String labelNumber = String.format("063S%012d", order.getId());
            rightCell.add(new Paragraph(labelNumber)
                .setFontSize(11)
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginBottom(3));
            rightCell.add(new Paragraph(String.format("%08d", 19926751 + order.getId()))
                .setFontSize(11)
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginBottom(10));
            
            String fromZip = order.getShippingZipCode();
            if (fromZip != null && fromZip.length() >= 5) {
                fromZip = fromZip.substring(0, 5);
            } else {
                fromZip = "80134";
            }
            
            rightCell.add(new Paragraph("FROM " + fromZip)
                .setFontSize(11)
                .setTextAlignment(TextAlignment.RIGHT)
                .setBold()
                .setMarginBottom(15));
            rightCell.add(new Paragraph(java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("MM/dd/yyyy")))
                .setFontSize(11)
                .setTextAlignment(TextAlignment.RIGHT));
            
            rightCell.setPadding(10)
                .setHeight(180)
                .setVerticalAlignment(VerticalAlignment.TOP)
                .setBorder(new SolidBorder(0));
            topSection.addCell(rightCell);
            
            document.add(topSection);
            
            // ========== SERVICE TYPE BANNER ==========
            Table serviceBanner = new Table(1);
            serviceBanner.setWidth(UnitValue.createPercentValue(100));
            serviceBanner.setMarginTop(0);
            serviceBanner.setMarginBottom(0);
            
            Cell serviceCell = new Cell();
            serviceCell.add(new Paragraph("USPS GROUND ADVANTAGE™")
                .setFontSize(32)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(ColorConstants.WHITE)
                .setBorder(new SolidBorder(ColorConstants.BLACK, 3))
                .setPadding(12);
            serviceBanner.addCell(serviceCell);
            document.add(serviceBanner);
            
            // ========== FROM SECTION ==========
            Table fromSection = new Table(new float[]{480, 100});
            fromSection.setWidth(UnitValue.createPercentValue(100));
            fromSection.setMarginTop(0);
            fromSection.setMarginBottom(0);
            fromSection.setBorder(new SolidBorder(ColorConstants.BLACK, 3));
            
            Cell fromCell = new Cell();
            fromCell.add(new Paragraph(order.getUser().getName())
                .setFontSize(22)
                .setBold()
                .setMarginBottom(3));
            fromCell.add(new Paragraph(order.getShippingStreet())
                .setFontSize(20)
                .setBold()
                .setMarginBottom(3));
            
            String cityStateZip = order.getShippingCity() + " " + order.getShippingState() + " " + order.getShippingZipCode();
            if (!cityStateZip.contains("–")) {
                cityStateZip += "–4145";
            }
            
            fromCell.add(new Paragraph(cityStateZip)
                .setFontSize(20)
                .setBold());
            fromCell.setPadding(15)
                .setHeight(120)
                .setVerticalAlignment(VerticalAlignment.TOP)
                .setBorderRight(new SolidBorder(ColorConstants.BLACK, 2))
                .setBorder(new SolidBorder(0));
            fromSection.addCell(fromCell);
            
            // Sequence number
            Cell seqCell = new Cell();
            seqCell.add(new Paragraph("0001")
                .setFontSize(28)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER))
                .setPadding(10)
                .setHeight(120)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBorder(new SolidBorder(0));
            fromSection.addCell(seqCell);
            
            document.add(fromSection);
            
            // ========== SHIP TO SECTION ==========
            Table shipToSection = new Table(new float[]{120, 460});
            shipToSection.setWidth(UnitValue.createPercentValue(100));
            shipToSection.setMarginTop(0);
            shipToSection.setMarginBottom(0);
            shipToSection.setBorder(new SolidBorder(ColorConstants.BLACK, 3));
            
            // QR Code on left
            Cell qrCell = new Cell();
            String qrData = "RETURN-ORDER-" + order.getId();
            byte[] qrCodeImg = generateQRCode(qrData, 110, 110);
            Image qrImage = new Image(ImageDataFactory.create(qrCodeImg));
            qrImage.setWidth(110);
            qrImage.setHeight(110);
            qrCell.add(qrImage);
            qrCell.setPadding(5)
                .setHeight(180)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBorderRight(new SolidBorder(ColorConstants.BLACK, 2))
                .setBorder(new SolidBorder(0));
            shipToSection.addCell(qrCell);
            
            // Ship to address on right
            Cell shipToCell = new Cell();
            shipToCell.add(new Paragraph("SHIP")
                .setFontSize(16)
                .setBold()
                .setMarginBottom(0));
            shipToCell.add(new Paragraph("TO:")
                .setFontSize(16)
                .setBold()
                .setMarginTop(0)
                .setMarginBottom(10));
            shipToCell.add(new Paragraph("E-COMMERCE RETURNS")
                .setFontSize(24)
                .setBold()
                .setMarginBottom(3));
            shipToCell.add(new Paragraph("970 17M")
                .setFontSize(24)
                .setBold()
                .setMarginBottom(3));
            shipToCell.add(new Paragraph("Middletown NY 10940")
                .setFontSize(24)
                .setBold());
            shipToCell.setPadding(15)
                .setHeight(180)
                .setVerticalAlignment(VerticalAlignment.TOP)
                .setBorder(new SolidBorder(0));
            shipToSection.addCell(shipToCell);
            
            document.add(shipToSection);
            
            // ========== BLACK SEPARATOR BAR ==========
            Table blackBar1 = new Table(1);
            blackBar1.setWidth(UnitValue.createPercentValue(100));
            blackBar1.setMarginTop(0);
            blackBar1.setMarginBottom(0);
            Cell blackCell1 = new Cell();
            blackCell1.setHeight(20);
            blackCell1.setBackgroundColor(ColorConstants.BLACK);
            blackCell1.setBorder(new SolidBorder(0));
            blackBar1.addCell(blackCell1);
            document.add(blackBar1);
            
            // ========== TRACKING NUMBER SECTION ==========
            Paragraph trackingHeader = new Paragraph("USPS TRACKING #")
                .setFontSize(24)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(20)
                .setMarginBottom(15);
            document.add(trackingHeader);
            
            // Generate main tracking barcode
            String trackingNumber = String.format("9400150105796054%010d", order.getId());
            byte[] trackingBarcodeImg = generateBarcode(trackingNumber, 540, 90);
            Image trackingBarcode = new Image(ImageDataFactory.create(trackingBarcodeImg));
            trackingBarcode.setWidth(540);
            trackingBarcode.setHeight(90);
            trackingBarcode.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
            document.add(trackingBarcode);
            
            // Tracking number text with spacing (format: 9400 1501 0579 6054 4386 34)
            String formattedTracking = trackingNumber.substring(0, 4) + " " +
                                      trackingNumber.substring(4, 8) + " " +
                                      trackingNumber.substring(8, 12) + " " +
                                      trackingNumber.substring(12, 16) + " " +
                                      trackingNumber.substring(16, 20) + " " +
                                      trackingNumber.substring(20);
            
            Paragraph trackingText = new Paragraph(formattedTracking)
                .setFontSize(22)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(8)
                .setMarginBottom(20);
            document.add(trackingText);
            
            // ========== BLACK SEPARATOR BAR ==========
            Table blackBar2 = new Table(1);
            blackBar2.setWidth(UnitValue.createPercentValue(100));
            blackBar2.setMarginTop(0);
            blackBar2.setMarginBottom(0);
            Cell blackCell2 = new Cell();
            blackCell2.setHeight(20);
            blackCell2.setBackgroundColor(ColorConstants.BLACK);
            blackCell2.setBorder(new SolidBorder(0));
            blackBar2.addCell(blackCell2);
            document.add(blackBar2);
            
            // ========== RMA NUMBER SECTION ==========
            Table rmaSection = new Table(new float[]{480, 100});
            rmaSection.setWidth(UnitValue.createPercentValue(100));
            rmaSection.setMarginTop(0);
            rmaSection.setBorder(new SolidBorder(ColorConstants.BLACK, 3));
            
            Cell rmaTextCell = new Cell();
            rmaTextCell.add(new Paragraph("RMA Number " + (1161 + order.getId()))
                .setFontSize(32)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER))
                .setPadding(25)
                .setHeight(120)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBorderRight(new SolidBorder(ColorConstants.BLACK, 2))
                .setBorder(new SolidBorder(0));
            rmaSection.addCell(rmaTextCell);
            
            // Small QR code on right
            Cell rmaQrCell = new Cell();
            byte[] smallQrImg = generateQRCode("RMA-" + order.getId() + "-" + trackingNumber, 90, 90);
            Image smallQr = new Image(ImageDataFactory.create(smallQrImg));
            smallQr.setWidth(90);
            smallQr.setHeight(90);
            rmaQrCell.add(smallQr);
            rmaQrCell.setPadding(5)
                .setHeight(120)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBorder(new SolidBorder(0));
            rmaSection.addCell(rmaQrCell);
            
            document.add(rmaSection);
            
            // ========== BOTTOM INSTRUCTIONS ==========
            Paragraph instructions = new Paragraph()
                .add("RETURN INSTRUCTIONS: ")
                .add("Securely pack items in original packaging. ")
                .add("Drop off at any USPS location. No postage required. ")
                .add("Refund processed within 5-7 business days after receipt. ")
                .add("Questions? Call 1-800-SHOP-NOW or email support@ecommerce.com")
                .setFontSize(8)
                .setMarginTop(15)
                .setTextAlignment(TextAlignment.CENTER)
                .setItalic();
            document.add(instructions);
            
            // Order details footer
            Paragraph orderDetails = new Paragraph()
                .add("Order #" + order.getId() + " | ")
                .add("Customer: " + order.getUser().getName() + " | ")
                .add("Items: " + order.getItems().size() + " | ")
                .add("Total: $" + String.format("%.2f", order.getTotalAmount()))
                .setFontSize(7)
                .setMarginTop(10)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(new DeviceRgb(100, 100, 100));
            document.add(orderDetails);
            
            System.out.println("✅ USPS-style shipping label generated successfully");
            
        } finally {
            document.close();
        }
        
        return baos.toByteArray();
    }

    private byte[] generateBarcode(String text, int width, int height) throws Exception {
        try {
            Code128Writer barcodeWriter = new Code128Writer();
            BitMatrix bitMatrix = barcodeWriter.encode(text, BarcodeFormat.CODE_128, width, height);
            
            BufferedImage barcodeImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(barcodeImage, "PNG", baos);
            
            return baos.toByteArray();
        } catch (Exception e) {
            System.err.println("❌ Error generating barcode: " + e.getMessage());
            throw e;
        }
    }

    private byte[] generateQRCode(String text, int width, int height) throws Exception {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);
            
            BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(qrImage, "PNG", baos);
            
            return baos.toByteArray();
        } catch (Exception e) {
            System.err.println("❌ Error generating QR code: " + e.getMessage());
            throw e;
        }
    }

    // Generate Base64 encoded label for preview in browser
    public String generateReturnShippingLabelBase64(Order order) throws Exception {
        byte[] pdfBytes = generateReturnShippingLabel(order);
        return Base64.getEncoder().encodeToString(pdfBytes);
    }
}