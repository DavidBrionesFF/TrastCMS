package com.bytecode.tratcms.controller.mvc.administrator;

import com.bytecode.tratcms.model.Grupo;
import com.bytecode.tratcms.repository.GrupoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/admin/grupo")
public class GrupoController {

    @Autowired
    private GrupoRepository grupoRepository;

    @GetMapping
    public ModelAndView getHome(
            @RequestParam(defaultValue = "all", required = false) String view_name,
            @RequestParam(defaultValue = "0", required = false) int id,
            SpringDataWebProperties.Pageable pageable
    ){
        ModelAndView modelAndView = new ModelAndView("administrator/grupo");
        switch (view_name){
            case "all":
                modelAndView.addObject("grupos", grupoRepository.findAll(pageable));
                break;
            case "new":
                modelAndView.addObject("grupo", new Grupo());
                break;
            case "update":
                modelAndView.addObject("grupo", grupoRepository.findById(id));
                break;
        }
        return modelAndView;
    }

    @PostMapping
    public String newAndUpdate(
            @ModelAttribute Grupo grupo
    ){
        if (grupo.getIdgrupo() > 0){
            grupoRepository.update(grupo);
        } else {
            grupoRepository.save(grupo);
        }
        return "redirect:/admin/grupo";
    }
}
