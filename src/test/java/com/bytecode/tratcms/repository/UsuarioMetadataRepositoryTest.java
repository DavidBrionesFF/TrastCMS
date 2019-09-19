package com.bytecode.tratcms.repository;

import com.bytecode.tratcms.component.TestDatabaseConfiguration;
import com.bytecode.tratcms.model.UsuarioMetadata;
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
public class UsuarioMetadataRepositoryTest {
    @Autowired
    private UsuarioMetadataRepository repository;

    @Test
    public void testA(){
        UsuarioMetadata usuarioMetadata = new UsuarioMetadata();
        usuarioMetadata.setClave("Edad");
        usuarioMetadata.setIdUsuario(1);
        usuarioMetadata.setTipo(Integer.class.getName());
        usuarioMetadata.setValor("18");
        usuarioMetadata.setIdUsuarioMetadata(1);

        Assert.assertTrue(repository.save(usuarioMetadata));
    }

    @Test
    public void testB(){
        UsuarioMetadata usuarioMetadata = new UsuarioMetadata();
        usuarioMetadata.setClave("Edad");
        usuarioMetadata.setIdUsuario(1);
        usuarioMetadata.setTipo(Integer.class.getName());
        usuarioMetadata.setValor("19");
        usuarioMetadata.setIdUsuarioMetadata(1);

        Assert.assertTrue(repository.update(usuarioMetadata));
    }

    @Test
    public void testC(){
        Assert.assertFalse(repository.findAll(new SpringDataWebProperties.Pageable()).isEmpty());
    }

    @Test
    public void testD(){
        Assert.assertTrue(repository.findById(1).getValor().equalsIgnoreCase("19"));
    }
}
