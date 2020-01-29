package com.bytecode.tratcms.controller.mvc.administrator;

import com.bytecode.tratcms.model.Post;
import com.bytecode.tratcms.model.PostMetadata;
import com.bytecode.tratcms.repository.CategoriaRepository;
import com.bytecode.tratcms.repository.PostMetadataRepository;
import com.bytecode.tratcms.repository.PostRepository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
                modelAndView.addObject("post", new Post());
                modelAndView.addObject("categorias", categoriaRepository.findAll(pageable));
                modelAndView.addObject("update", true);
                modelAndView.addObject("meta_description", new PostMetadata());
                break;
            case "update":
                List<PostMetadata> postMetadatas = postMetadataRepository.findByIdPost(id);
                modelAndView.addObject("post", postRepository.findById(id));
                modelAndView.addObject("categorias", categoriaRepository.findAll(pageable));
                modelAndView.addObject("update", true);
                modelAndView.addObject("post_metadata", postMetadatas);
                modelAndView.addObject("meta_description", postMetadatas.stream()
                                                                         .filter(postMetadata -> {
                                                                             return postMetadata.getClave().equalsIgnoreCase("meta_descripcion");
                                                                         }).collect(Collectors.toList()).get(0));
                break;
        }
        return modelAndView;
    }

    @PostMapping
    public String addAndUpdatePublicacion(@ModelAttribute Post post,
                                          @RequestParam(name = "meta_descripcion_text_html") String descripcion,
                                          @RequestParam(name = "meta_id", defaultValue = "0") int idMeta){
        PostMetadata postMetadata = new PostMetadata();
        postMetadata.setIdPost(post.getIdPost());
        postMetadata.setClave("meta_descripcion");
        postMetadata.setValor(descripcion);
        postMetadata.setTipo("text/html");
        postMetadata.setIdPostMetadata(idMeta);

        if (post.getIdPost() > 0){
            postRepository.update(post);
            postMetadataRepository.update(postMetadata);
        } else {
            post = postRepository.findOnSave(post);
            postMetadata.setIdPost(post.getIdPost());
            postMetadataRepository.save(postMetadata);
        }
        return "redirect:/admin/publicacion";
    }
}
