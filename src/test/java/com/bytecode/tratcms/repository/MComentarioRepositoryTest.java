package com.bytecode.tratcms.repository;

import com.bytecode.tratcms.component.TestDatabaseConfiguration;
import com.bytecode.tratcms.model.MComentario;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@ContextConfiguration(classes = {TestDatabaseConfiguration.class})
public class MComentarioRepositoryTest {
    @Autowired
    private ComentarioRepository repository;

    @Test
    public void testA(){
        MComentario MComentario = new MComentario();
        MComentario.setComentario("ComentarioA");
        MComentario.setIdComentario(1);
        MComentario.setIdPost(3);
        MComentario.setIdUsuario(1);
        MComentario.setRespuesta(null);

        Assert.assertTrue(repository.save(MComentario));
    }

    @Test
    public void testB(){
        MComentario MComentario = new MComentario();
        MComentario.setComentario("ComentarioB");
        MComentario.setIdComentario(1);
        MComentario.setIdPost(3);
        MComentario.setIdUsuario(1);
        MComentario.setRespuesta(null);

        Assert.assertTrue(repository.update(MComentario));
    }

    @Test
    public void testC(){
        Assert.assertFalse(repository.findAll(new SpringDataWebProperties.Pageable()).isEmpty());
    }

    @Test
    public void testD(){
        Assert.assertTrue(repository.findById(1).getComentario().equalsIgnoreCase("ComentarioB"));
    }
}
