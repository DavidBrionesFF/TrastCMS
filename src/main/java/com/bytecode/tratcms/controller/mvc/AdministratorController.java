package com.bytecode.tratcms.controller.mvc;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/admin")
public class AdministratorController {
    @GetMapping("/")
    public ModelAndView getHome(){
        return new ModelAndView("administrator/admin");
    }

    @GetMapping("/blank")
    public ModelAndView getBlank(){
        return new ModelAndView("administrator/blank");
    }
}
