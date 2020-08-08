package com.bytecode.tratcms.repository;

import com.bytecode.tratcms.component.TestDatabaseConfiguration;
import com.bytecode.tratcms.model.MCategoria;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.annotation.Order;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {TestDatabaseConfiguration.class})
public class MCategoriaRepositoryTest {

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Test
    @Order(1)
    public void testInsert(){
        MCategoria MCategoria = new MCategoria();

        MCategoria.setNombre("Test1");
        MCategoria.setDescripcion("Este es un ejemplo de categoria superior");
        MCategoria.setCategoriaSuperior(1);

        boolean result = categoriaRepository.save(MCategoria);

        Assert.assertTrue(result);
    }

    @Test()
    @Order(2)
    public void testUpdate(){
        MCategoria MCategoria = new MCategoria();

        MCategoria.setIdCategoria(1);
        MCategoria.setNombre("Test2");
        MCategoria.setDescripcion("Este es un ejemplo de categoria superior");
        MCategoria.setCategoriaSuperior(1);

        boolean result = categoriaRepository.update(MCategoria);

        Assert.assertTrue(result);
    }

    @Test
    @Order(3)
    public void testFindById(){
        MCategoria MCategoria = categoriaRepository.findById(1);

        Assert.assertTrue(MCategoria !=null);
        Assert.assertTrue("Test2".equals(MCategoria.getNombre()));
    }

    @Test
    @Order(4)
    public void testFindAll(){
        SpringDataWebProperties.Pageable pageable = new SpringDataWebProperties.Pageable();
        Assert.assertFalse(categoriaRepository.findAll(pageable).isEmpty());
    }
}
