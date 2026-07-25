package com.nattechnologies.trastcms.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {

    @GetMapping({
        "/",
        "/store",
        "/store/**",
        "/cart",
        "/pricing",
        "/developers",
        "/saas/**",
        "/post/**",
        "/page/**",
        "/category/**",
        "/admin",
        "/admin/**"
    })
    public String index() {
        return "forward:/index.html";
    }

    @GetMapping(value = "/{path:^(?!api|actuator|swagger|v3|assets|favicon\\.ico|index\\.html|error).*$}/**")
    public String fallback() {
        return "forward:/index.html";
    }
}
