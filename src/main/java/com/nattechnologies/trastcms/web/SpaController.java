package com.nattechnologies.trastcms.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {
    @GetMapping({"/", "/admin", "/post/{slug}", "/page/{slug}", "/category/{slug}", "/developers"})
    public String index() { return "forward:/index.html"; }

    @GetMapping("/admin/{*path}")
    public String admin() { return "forward:/index.html"; }
}
