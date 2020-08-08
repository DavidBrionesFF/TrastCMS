package com.bytecode.tratcms.controller.mvc.administrator;

import com.bytecode.tratcms.model.MPost;
import com.bytecode.tratcms.model.MPostMetadata;
import com.bytecode.tratcms.repository.CategoriaRepository;
import com.bytecode.tratcms.repository.PostMetadataRepository;
import com.bytecode.tratcms.repository.PostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/publicacion")
public class PublicacionController {
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private CategoriaRepository categoriaRepository;
    @Autowired
    private PostMetadataRepository postMetadataRepository;

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
                modelAndView.addObject("update", false);
                break;
            case "new":
                modelAndView.addObject("post", new MPost());
                modelAndView.addObject("categorias", categoriaRepository.findAll(pageable));
                modelAndView.addObject("update", true);
                modelAndView.addObject("meta_description", new MPostMetadata());
                break;
            case "update":
                List<MPostMetadata> MPostMetadata = postMetadataRepository.findByIdPost(id);
                modelAndView.addObject("post", postRepository.findById(id));
                modelAndView.addObject("categorias", categoriaRepository.findAll(pageable));
                modelAndView.addObject("update", true);
                modelAndView.addObject("post_metadata", MPostMetadata);
                modelAndView.addObject("meta_description", MPostMetadata.stream()
                                                                         .filter(postMetadata -> {
                                                                             return postMetadata.getClave().equalsIgnoreCase("meta_descripcion");
                                                                         }).collect(Collectors.toList()).get(0));
                break;
        }
        return modelAndView;
    }

    @PostMapping
    public String addAndUpdatePublicacion(@ModelAttribute MPost MPost,
                                          @RequestParam(name = "meta_descripcion_text_html") String descripcion,
                                          @RequestParam(name = "meta_id", defaultValue = "0") int idMeta){
        MPostMetadata MPostMetadata = new MPostMetadata();
        MPostMetadata.setIdPost(MPost.getIdPost());
        MPostMetadata.setClave("meta_descripcion");
        MPostMetadata.setValor(descripcion);
        MPostMetadata.setTipo("text/html");
        MPostMetadata.setIdPostMetadata(idMeta);

        if (MPost.getIdPost() > 0){
            postRepository.update(MPost);
            postMetadataRepository.update(MPostMetadata);
        } else {
            MPost = postRepository.findOnSave(MPost);
            MPostMetadata.setIdPost(MPost.getIdPost());
            postMetadataRepository.save(MPostMetadata);
        }
        return "redirect:/admin/publicacion";
    }
}
