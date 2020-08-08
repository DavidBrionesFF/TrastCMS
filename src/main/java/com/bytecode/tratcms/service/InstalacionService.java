package com.bytecode.tratcms.service;

import com.bytecode.tratcms.model.MUsuario;
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
        MUsuario MUsuario = null;
        try {
            usuarioRepository.findByCorreo(env.getProperty("bytecode.usuario.correo"));
        }catch (Exception e){
            MUsuario = new MUsuario();
            MUsuario.setContrasena(passwordEncoder.encode(env.getProperty("bytecode.usuario.contrasena")));
            MUsuario.setApellido("Administrador");
            MUsuario.setNombre("Administrador");
            MUsuario.setIdGrupo(1);
            MUsuario.setCorreo(env.getProperty("bytecode.usuario.correo"));
            usuarioRepository.save(MUsuario);
        }
    }
}
