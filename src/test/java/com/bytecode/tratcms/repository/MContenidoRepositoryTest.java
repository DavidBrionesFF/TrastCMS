package com.bytecode.tratcms.repository;

import com.bytecode.tratcms.component.TestDatabaseConfiguration;
import com.bytecode.tratcms.model.MContenido;
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
public class MContenidoRepositoryTest {

    @Autowired
    private ContenidoRepository repository;

    @Test
    public void testA(){
        MContenido MContenido = new MContenido();
        MContenido.setContenido("Hola");
        MContenido.setIdPost(3);
        MContenido.setTipo(String.class.getName());
        MContenido.setIdContenido(1);

        Assert.assertTrue(repository.save(MContenido));
    }

    @Test
    public void testB(){
        MContenido MContenido = new MContenido();
        MContenido.setContenido("HolaAA");
        MContenido.setIdPost(3);
        MContenido.setTipo(String.class.getName());
        MContenido.setIdContenido(3);

        Assert.assertTrue(repository.update(MContenido));
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
