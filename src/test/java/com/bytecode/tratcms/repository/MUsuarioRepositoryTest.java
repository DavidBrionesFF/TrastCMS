package com.bytecode.tratcms.repository;

import com.bytecode.tratcms.component.TestDatabaseConfiguration;
import com.bytecode.tratcms.model.MUsuario;
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
public class MUsuarioRepositoryTest {
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    public void testA(){
        MUsuario MUsuario = new MUsuario();
        MUsuario.setApellido("Briones");
        MUsuario.setContrasena("1234");
        MUsuario.setCorreo("david_briones@bytepl.com");
        MUsuario.setIdGrupo(1);
        MUsuario.setNombre("David");

        Assert.assertTrue(usuarioRepository.save(MUsuario));
    }

    @Test
    public void testB(){
        MUsuario MUsuario = new MUsuario();
        MUsuario.setIdUsuario(1);
        MUsuario.setApellido("Briones");
        MUsuario.setContrasena("1234");
        MUsuario.setCorreo("aprendefacil1020@gmail.com");
        MUsuario.setIdGrupo(1);
        MUsuario.setNombre("DavidBB");

        Assert.assertTrue(usuarioRepository.update(MUsuario));
    }

    @Test
    public void testC(){
        Assert.assertFalse(usuarioRepository.findAll(new SpringDataWebProperties.Pageable()).isEmpty());
    }

    @Test
    public void testD(){
        Assert.assertTrue(usuarioRepository.findById(1).getNombre().equalsIgnoreCase("DavidBB"));
    }
}
