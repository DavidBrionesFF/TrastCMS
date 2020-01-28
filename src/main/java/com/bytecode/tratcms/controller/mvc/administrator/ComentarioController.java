package com.bytecode.tratcms.controller.mvc.administrator;

import com.bytecode.tratcms.model.Comentario;
import com.bytecode.tratcms.repository.ComentarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/admin/comentario")
public class ComentarioController {
    @Autowired
    private ComentarioRepository comentarioRepository;

    @GetMapping
    public ModelAndView getHome(
            @RequestParam(defaultValue = "all", required = false) String view_name,
            @RequestParam(defaultValue = "0", required = false) int id,
            SpringDataWebProperties.Pageable pageable
    ){
        ModelAndView modelAndView = new ModelAndView("administrator/comentario");
        switch (view_name) {
            case "all":
                modelAndView.addObject("comentarios", comentarioRepository.findAll(pageable));
                break;
            case "new":
                modelAndView.addObject("comentario", new Comentario());
                break;
            case "update":
                modelAndView.addObject("comentario", comentarioRepository.findById(id));
                break;
        }
        return modelAndView;
    }
}
