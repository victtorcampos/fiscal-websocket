package tech.vcinf.fiscalwebsocket.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TestController {

    @GetMapping("/teste")
    public String teste() {
        return "teste.html";
    }
}
