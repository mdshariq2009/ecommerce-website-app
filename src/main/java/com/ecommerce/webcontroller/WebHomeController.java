package com.ecommerce.webcontroller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebHomeController {
    
    @GetMapping("/")
    public String home() {
        return "index";
    }
    
    @GetMapping("/web/login")
    public String login() {
        return "login";
    }
    
    @GetMapping("/web/register")
    public String register() {
        return "register";
    }
    
    @GetMapping("/web/products")
    public String products() {
        return "products";
    }
    
    @GetMapping("/web/cart")
    public String cart() {
        return "cart";
    }
    
    @GetMapping("/web/checkout")
    public String checkout() {
        return "checkout";
    }
    
    @GetMapping("/web/stripe-checkout")
    public String stripeCheckout() {
        return "stripe-checkout";
    }
    
    @GetMapping("/web/orders")
    public String orders() {
        return "orders";
    }
    
    @GetMapping("/web/admin")
    public String admin() {
        return "admin";
    }
    @GetMapping("/web/profile")
    public String profile() {
        return "profile";
    }
    @GetMapping("/web/product-details")
    public String productDetails() {
        return "product-details";
    }
    @GetMapping("/web/contact")
    public String contactUs() {
        return "contact-us";
    }
    
    @GetMapping("/web/returns")
    public String returnsRefunds() {
        return "returns-refunds";
    }
    
    @GetMapping("/web/privacy")
    public String privacyPolicy() {
        return "privacy-policy";
    }
    

}
