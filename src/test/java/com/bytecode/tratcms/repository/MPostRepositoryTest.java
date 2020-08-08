package com.bytecode.tratcms.repository;

import com.bytecode.tratcms.component.TestDatabaseConfiguration;
import com.bytecode.tratcms.init.InitConfiguration;
import com.bytecode.tratcms.model.MPost;
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
public class MPostRepositoryTest {
    @Autowired
    private PostRepository postRepository;

    @Autowired
    private InitConfiguration initConfiguration;

    @Test
    public void insert() throws IOException {
        MPost MPost = new MPost();
        MPost.setIdPost(1);
        MPost.setImagenDestacada("image.jpg");
        MPost.setCategoria(1);
        MPost.setExtracto("Extracto de ejemplo");
        MPost.setSlug("nuevo-post");
        MPost.setTitulo("Nuevo Post");
        MPost.setTipo(initConfiguration.getTypes().get(1));
        MPost.setIdUsuario(1);

        boolean result = postRepository.save(MPost);

        Assert.assertTrue(result);
    }

    @Test
    public void update() throws IOException {
        MPost MPost = new MPost();
        MPost.setIdPost(1);
        MPost.setImagenDestacada("image.jpg");
        MPost.setCategoria(1);
        MPost.setExtracto("Extracto de ejemplo");
        MPost.setSlug("nuevo-post-xd");
        MPost.setTitulo("Nuevo Post XD");
        MPost.setTipo(initConfiguration.getTypes().get(1));
        MPost.setIdUsuario(1);

        boolean result = postRepository.update(MPost);

        Assert.assertTrue(result);
    }

    @Test
    public void findById(){
        MPost MPost = postRepository.findById(3);
        Assert.assertNotNull(MPost);
    }

    @Test
    public void findAll(){
        SpringDataWebProperties.Pageable pageable = new SpringDataWebProperties.Pageable();
        Assert.assertFalse(postRepository.findAll(pageable).isEmpty());
    }
}
