package com.bytecode.tratcms.repository;

import com.bytecode.tratcms.component.TestDatabaseConfiguration;
import com.bytecode.tratcms.model.Contenido;
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
public class ContenidoRepositoryTest {

    @Autowired
    private ContenidoRepository repository;

    @Test
    public void testA(){
        Contenido contenido = new Contenido();
        contenido.setContenido("Hola");
        contenido.setIdPost(3);
        contenido.setTipo(String.class.getName());
        contenido.setIdContenido(1);

        Assert.assertTrue(repository.save(contenido));
    }

    @Test
    public void testB(){
        Contenido contenido = new Contenido();
        contenido.setContenido("HolaAA");
        contenido.setIdPost(3);
        contenido.setTipo(String.class.getName());
        contenido.setIdContenido(3);

        Assert.assertTrue(repository.update(contenido));
    }

    @Test
    public void testC(){
        Assert.assertFalse(repository.findAll(new SpringDataWebProperties.Pageable()).isEmpty());
    }

    @Test
    public void testD(){
        Assert.assertTrue(repository.findById(3).getContenido().equalsIgnoreCase("HolaAA"));
    }
}
