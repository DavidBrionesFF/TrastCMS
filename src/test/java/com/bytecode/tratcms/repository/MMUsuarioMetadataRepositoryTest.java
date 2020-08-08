package com.bytecode.tratcms.repository;

import com.bytecode.tratcms.component.TestDatabaseConfiguration;
import com.bytecode.tratcms.model.MUsuarioMetadata;
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
public class MMUsuarioMetadataRepositoryTest {
    @Autowired
    private UsuarioMetadataRepository repository;

    @Test
    public void testA(){
        MUsuarioMetadata MUsuarioMetadata = new MUsuarioMetadata();
        MUsuarioMetadata.setClave("Edad");
        MUsuarioMetadata.setIdUsuario(1);
        MUsuarioMetadata.setTipo(Integer.class.getName());
        MUsuarioMetadata.setValor("18");
        MUsuarioMetadata.setIdUsuarioMetadata(1);

        Assert.assertTrue(repository.save(MUsuarioMetadata));
    }

    @Test
    public void testB(){
        MUsuarioMetadata MUsuarioMetadata = new MUsuarioMetadata();
        MUsuarioMetadata.setClave("Edad");
        MUsuarioMetadata.setIdUsuario(1);
        MUsuarioMetadata.setTipo(Integer.class.getName());
        MUsuarioMetadata.setValor("19");
        MUsuarioMetadata.setIdUsuarioMetadata(1);

        Assert.assertTrue(repository.update(MUsuarioMetadata));
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
