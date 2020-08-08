package com.bytecode.tratcms.service;

import com.bytecode.tratcms.model.MGrupo;
import com.bytecode.tratcms.model.MUsuario;
import com.bytecode.tratcms.repository.GrupoRepository;
import com.bytecode.tratcms.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
public class UsuarioService implements UserDetailsService {
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private GrupoRepository grupoRepository;

    @Override
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        MUsuario MUsuario = usuarioRepository.findByCorreo(correo);
        MGrupo MGrupo = grupoRepository.findById(Integer.valueOf(MUsuario.getIdGrupo() + ""));
        GrantedAuthority grantedAuthority = new SimpleGrantedAuthority(("ROLE_" + MGrupo.getNombre()).toUpperCase());
        return new User(MUsuario.getCorreo(), MUsuario.getContrasena(), Arrays.asList(grantedAuthority));
    }
}
