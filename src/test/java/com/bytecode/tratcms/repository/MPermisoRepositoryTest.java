package com.bytecode.tratcms.repository;

import com.bytecode.tratcms.component.TestDatabaseConfiguration;
import com.bytecode.tratcms.model.MPermiso;
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
public class MPermisoRepositoryTest {
    @Autowired
    private PermisoRepository repository;

    @Test
    public void testA(){
        MPermiso MPermiso = new MPermiso();
        MPermiso.setIdPermiso(1);
        MPermiso.setNombre("Nuevo Permismo");

        Assert.assertTrue(repository.save(MPermiso));
    }

    @Test
    public void testB(){
        MPermiso MPermiso = new MPermiso();
        MPermiso.setIdPermiso(1);
        MPermiso.setNombre("Nuevo Permismo2");

        Assert.assertTrue(repository.update(MPermiso));
    }

    @Test
    public void testC(){
        Assert.assertFalse(repository.findAll(new SpringDataWebProperties.Pageable()).isEmpty());
    }

    @Test
    public void testD(){
        Assert.assertTrue(repository.findById(1).getNombre().equalsIgnoreCase("Nuevo Permismo2"));
    }
}
