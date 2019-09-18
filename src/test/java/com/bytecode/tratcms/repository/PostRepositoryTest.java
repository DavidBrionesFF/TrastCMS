package com.bytecode.tratcms.repository;

import com.bytecode.tratcms.component.TestDatabaseConfiguration;
import com.bytecode.tratcms.init.InitConfiguration;
import com.bytecode.tratcms.model.Post;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {TestDatabaseConfiguration.class})
public class PostRepositoryTest {
    @Autowired
    private PostRepository postRepository;

    @Autowired
    private InitConfiguration initConfiguration;

    @Test
    public void insert() throws IOException {
        Post post = new Post();
        post.setIdPost(1);
        post.setImagenDestacada("image.jpg");
        post.setCategoria(1);
        post.setExtracto("Extracto de ejemplo");
        post.setSlug("nuevo-post");
        post.setTitulo("Nuevo Post");
        post.setTipo(initConfiguration.getTypes().get(1));
        post.setIdUsuario(1);

        boolean result = postRepository.save(post);

        Assert.assertTrue(result);
    }

    @Test
    public void update() throws IOException {
        Post post = new Post();
        post.setIdPost(1);
        post.setImagenDestacada("image.jpg");
        post.setCategoria(1);
        post.setExtracto("Extracto de ejemplo");
        post.setSlug("nuevo-post-xd");
        post.setTitulo("Nuevo Post XD");
        post.setTipo(initConfiguration.getTypes().get(1));
        post.setIdUsuario(1);

        boolean result = postRepository.update(post);

        Assert.assertTrue(result);
    }

    @Test
    public void findById(){
        Post post = postRepository.findById(3);
        Assert.assertNotNull(post);
    }

    @Test
    public void findAll(){
        SpringDataWebProperties.Pageable pageable = new SpringDataWebProperties.Pageable();
        Assert.assertFalse(postRepository.findAll(pageable).isEmpty());
    }
}
