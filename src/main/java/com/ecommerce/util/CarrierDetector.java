package com.ecommerce.util;

public class CarrierDetector {

    public static String detectCarrier(String trackingNumber) {
        if (trackingNumber == null || trackingNumber.trim().isEmpty()) {
            return "USPS";
        }
        
        trackingNumber = trackingNumber.trim().toUpperCase().replaceAll("\\s+", "");
        
        // UPS Detection
        if (isUPS(trackingNumber)) {
            return "UPS";
        }
        
        // FedEx Detection
        if (isFedEx(trackingNumber)) {
            return "FedEx";
        }
        
        // DHL Detection
        if (isDHL(trackingNumber)) {
            return "DHL";
        }
        
        // USPS Detection
        if (isUSPS(trackingNumber)) {
            return "USPS";
        }
        
        // Amazon Logistics
        if (isAmazon(trackingNumber)) {
            return "Amazon Logistics";
        }
        
        // Default to USPS if no match
        return "USPS";
    }
    
    private static boolean isUPS(String tracking) {
        // UPS tracking numbers:
        // - 1Z followed by 16 characters (1Z999AA10123456784)
        // - 18 digits starting with specific prefixes
        // - T followed by 10 digits
        
    	 if (tracking.matches("^(94|92|93|82|71|61|42|41|40|23|03|02|01)\\d{18,24}$")) {
    	        return true;
    	    }
        
    	 if (tracking.matches("^[A-Z]{2}\\d{9}US$")) {
    	        return true;
    	    }
        
        if (tracking.matches("^\\d{18}$")) {
            return true;
        }
        
        return false;
    }
    
    private static boolean isFedEx(String tracking) {
        // FedEx tracking numbers:
        // - 12 digits (9612804510001234)
        // - 15 digits (012345678901234)
        // - 20 digits (01234567890123456789)
        // - 22 digits (0123456789012345678901)
        
        int length = tracking.length();
        
        if (tracking.matches("^\\d+$")) {
            // 12 digits - but exclude USPS patterns
            if (length == 12 && !tracking.matches("^(94|92|93|82|71|61|42|41|40|23|03|02|01).*")) {
                return true;
            }
            
            // 14, 15, 20, 22 digits
            if (length == 14 || length == 15 || length == 20 || length == 22) {
                return true;
            }
        }
        
        // FedEx Express: starts with 96, 02-09 (but NOT 94, 92, 93)
        if (tracking.matches("^(96|02|03|04|05|06|07|08|09)\\d{10,20}$")) {
            return true;
        }
        
        return false;
    }
    
    private static boolean isDHL(String tracking) {
        // DHL tracking numbers:
        // - 10 digits (1234567890)
        // - 11 digits starting with specific numbers
        // - Starts with JJD, JVGL, or other DHL prefixes
        
        if (tracking.matches("^\\d{10,11}$")) {
            return true;
        }
        
        if (tracking.matches("^(JJD|JVGL|GM|LX|RX)\\d{10,20}$")) {
            return true;
        }
        
        return false;
    }
    
    private static boolean isUSPS(String tracking) {
        // USPS tracking numbers:
        // - 20 digits (94001234567890123456)
        // - 22 digits
        // - 26 digits
        // - Starts with 94, 92, 93, 82, 71, 61, 42, 41, 40, 23, 03, 02, 01
        // - Priority Mail: 9400, 9205, 9303
        // - Certified Mail: 9407, 9208
        
        if (tracking.matches("^(94|92|93|82|71|61|42|41|40|23|03|02|01)\\d{18,24}$")) {
            return true;
        }
        
        if (tracking.matches("^[A-Z]{2}\\d{9}US$")) {
            return true;
        }
        
        return false;
    }
    
    private static boolean isAmazon(String tracking) {
        // Amazon Logistics tracking:
        // - TBA followed by 12-13 digits
        
        if (tracking.matches("^TBA\\d{12,13}$")) {
            return true;
        }
        
        return false;
    }
    
    public static String getTrackingUrl(String trackingNumber, String carrier) {
        if (trackingNumber == null || trackingNumber.trim().isEmpty()) {
            return "#";
        }
        
        trackingNumber = trackingNumber.trim();
        
        switch (carrier.toUpperCase()) {
            case "UPS":
                return "https://www.ups.com/track?tracknum=" + trackingNumber;
                
            case "FEDEX":
                return "https://www.fedex.com/fedextrack/?trknbr=" + trackingNumber;
                
            case "DHL":
                return "https://www.dhl.com/en/express/tracking.html?AWB=" + trackingNumber;
                
            case "USPS":
                return "https://tools.usps.com/go/TrackConfirmAction?tLabels=" + trackingNumber;
                
            case "AMAZON LOGISTICS":
                return "https://track.amazon.com/tracking/" + trackingNumber;
                
            default:
                return "#";
        }
    }
}