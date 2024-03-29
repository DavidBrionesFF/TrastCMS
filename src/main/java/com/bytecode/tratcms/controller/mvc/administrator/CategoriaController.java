package com.bytecode.tratcms.controller.mvc.administrator;

import com.bytecode.tratcms.data.model.MCategoria;
import com.bytecode.tratcms.data.repository.CategoriaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/admin/categoria")
public class CategoriaController {

    @Autowired
    private CategoriaRepository categoriaRepository;

    @GetMapping
    public ModelAndView getHome(
            @RequestParam(defaultValue = "all", required = false) String view_name,
            @RequestParam(defaultValue = "0", required = false) int id,
            SpringDataWebProperties.Pageable pageable
    ){
        ModelAndView modelAndView = new ModelAndView("administrator/categoria");
        switch (view_name){
            case "all":
                modelAndView.addObject("categorias", categoriaRepository.findAll(pageable));
                break;
            case "new":
                modelAndView.addObject("categoria", new MCategoria());
                break;
            case "update":
                modelAndView.addObject("categoria", categoriaRepository.findById(id));
                break;
        }
        return modelAndView;
    }

    @PostMapping
    public String newAndUpdate(
            @ModelAttribute MCategoria MCategoria
    ){
        if (MCategoria.getIdCategoria() > 0){
            categoriaRepository.update(MCategoria);
        } else {
            categoriaRepository.save(MCategoria);
        }
        return "redirect:/admin/categoria";
    }

    @DeleteMapping
    public String deleteById(
            @RequestParam int id
    ){
        categoriaRepository.deleteById(id);
        return "redirect:/admin/categoria";
    }
}
