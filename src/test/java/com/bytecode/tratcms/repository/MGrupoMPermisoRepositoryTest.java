package com.bytecode.tratcms.repository;

import com.bytecode.tratcms.component.TestDatabaseConfiguration;
import com.bytecode.tratcms.model.MGrupoPermiso;
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
public class MGrupoMPermisoRepositoryTest {
    @Autowired
    private GrupoPermisoRepository grupoPermisoRepository;

    @Test
    public void testA(){
        MGrupoPermiso MGrupoPermiso = new MGrupoPermiso();
        MGrupoPermiso.setIdGrupo(1);
        MGrupoPermiso.setIdPermiso(1);
        Assert.assertTrue(grupoPermisoRepository.save(MGrupoPermiso));
    }

    @Test
    public void testB(){
        MGrupoPermiso MGrupoPermiso = new MGrupoPermiso();
        MGrupoPermiso.setIdGrupo(1);
        MGrupoPermiso.setIdPermiso(2);
        Assert.assertTrue(grupoPermisoRepository.update(MGrupoPermiso));
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
