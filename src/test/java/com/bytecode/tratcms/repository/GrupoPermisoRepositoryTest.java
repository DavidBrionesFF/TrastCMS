package com.bytecode.tratcms.repository;

import com.bytecode.tratcms.component.TestDatabaseConfiguration;
import com.bytecode.tratcms.model.GrupoPermiso;
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
public class GrupoPermisoRepositoryTest {
    @Autowired
    private GrupoPermisoRepository grupoPermisoRepository;

    @Test
    public void testA(){
        GrupoPermiso grupoPermiso = new GrupoPermiso();
        grupoPermiso.setIdGrupo(1);
        grupoPermiso.setIdPermiso(1);
        Assert.assertTrue(grupoPermisoRepository.save(grupoPermiso));
    }

    @Test
    public void testB(){
        GrupoPermiso grupoPermiso = new GrupoPermiso();
        grupoPermiso.setIdGrupo(1);
        grupoPermiso.setIdPermiso(2);
        Assert.assertTrue(grupoPermisoRepository.update(grupoPermiso));
    }

    @Test
    public void testC(){
        Assert.assertFalse(grupoPermisoRepository.findAll(new SpringDataWebProperties.Pageable()).isEmpty());
    }

    @Test
    public void testD(){
        Assert.assertTrue(grupoPermisoRepository.findById(1).getIdPermiso()==1);
    }
}
