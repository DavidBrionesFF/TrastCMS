package com.bytecode.tratcms.core.service;

import com.bytecode.tratcms.core.model.Usuario;
import com.bytecode.tratcms.core.repository.UsuarioRep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class UsuarioService implements UserDetailsService {
    @Autowired
    private UsuarioRep usuarioRep;

    @Override
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        Usuario usuario = usuarioRep.findByCorreo(correo);
        if (usuario==null){
            throw new UsernameNotFoundException("No existe usuario con ese correo");
        }
        return new User(usuario.getCorreo(), usuario.getContrasena(), new ArrayList<>());
    }
}
