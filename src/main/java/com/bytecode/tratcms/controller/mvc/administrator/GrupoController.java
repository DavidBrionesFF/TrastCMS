package com.bytecode.tratcms.controller.mvc.administrator;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/admin/grupo")
public class GrupoController {
    @GetMapping
    public ModelAndView getHome(){
        return new ModelAndView("administrator/grupo");
    }
}
