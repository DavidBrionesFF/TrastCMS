package com.bytecode.tratcms.controller.mvc.administrator;

import com.bytecode.tratcms.model.Post;
import com.bytecode.tratcms.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/admin/publicacion")
public class PublicacionController {
    @Autowired
    private PostRepository postRepository;

    @GetMapping
    public ModelAndView getHome(
            @RequestParam(defaultValue = "all", required = false) String view_name,
            @RequestParam(defaultValue = "0", required = false) int id,
            SpringDataWebProperties.Pageable pageable
    ){
        ModelAndView modelAndView = new ModelAndView("administrator/publicacion");
        switch (view_name){
            case "all":
                modelAndView.addObject("posts", postRepository.findAll(pageable));
                break;
            case "new":
                modelAndView.addObject("post", new Post());
                break;
            case "update":
                modelAndView.addObject("post", postRepository.findById(id));
                break;
        }
        return modelAndView;
    }
}
