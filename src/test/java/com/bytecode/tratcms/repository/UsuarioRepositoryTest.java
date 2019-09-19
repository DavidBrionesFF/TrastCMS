package com.bytecode.tratcms.repository;

import com.bytecode.tratcms.component.TestDatabaseConfiguration;
import com.bytecode.tratcms.model.Usuario;
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
public class UsuarioRepositoryTest {
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    public void testA(){
        Usuario usuario = new Usuario();
        usuario.setApellido("Briones");
        usuario.setContrasena("1234");
        usuario.setCorreo("david_briones@bytepl.com");
        usuario.setIdGrupo(1);
        usuario.setNombre("David");

        Assert.assertTrue(usuarioRepository.save(usuario));
    }

    @Test
    public void testB(){
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(1);
        usuario.setApellido("Briones");
        usuario.setContrasena("1234");
        usuario.setCorreo("aprendefacil1020@gmail.com");
        usuario.setIdGrupo(1);
        usuario.setNombre("DavidBB");

        Assert.assertTrue(usuarioRepository.update(usuario));
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
