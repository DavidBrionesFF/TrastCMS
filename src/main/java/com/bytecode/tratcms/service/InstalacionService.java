package com.bytecode.tratcms.service;

import com.bytecode.tratcms.model.Grupo;
import com.bytecode.tratcms.model.Usuario;
import com.bytecode.tratcms.repository.GrupoRepository;
import com.bytecode.tratcms.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class InstalacionService {
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private Environment env;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    public void init_usuarios(){
        Usuario usuario = null;
        try {
            usuarioRepository.findByCorreo(env.getProperty("bytecode.usuario.correo"));
        }catch (Exception e){
            usuario = new Usuario();
            usuario.setContrasena(passwordEncoder.encode(env.getProperty("bytecode.usuario.contrasena")));
            usuario.setApellido("Administrador");
            usuario.setNombre("Administrador");
            usuario.setIdGrupo(1);
            usuario.setCorreo(env.getProperty("bytecode.usuario.correo"));
            usuarioRepository.save(usuario);
        }
    }
}
