package com.ecommerce.webcontroller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminLiveChatController {
    
    @GetMapping("/live-chat")
    public String liveChatDashboard() {
        return "admin/live-chat";
    }
}
