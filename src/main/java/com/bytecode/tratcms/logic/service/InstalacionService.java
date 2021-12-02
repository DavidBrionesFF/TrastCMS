package com.bytecode.tratcms.logic.service;

import com.bytecode.tratcms.data.model.MUsuario;
import com.bytecode.tratcms.data.repository.UsuarioRepository;
import com.bytecode.tratcms.logic.service.instalacion.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class InstalacionService {
    @Autowired
    @Qualifier("PermisoInstalacionService")
    private PermisoInstalacionService permisoInstalacionService;

    @Autowired
    @Qualifier("GrupoInstalacionService")
    private GrupoInstalacionService grupoInstalacionService;

    @Autowired
    @Qualifier("GrupoPermisoInstalacionService")
    private GrupoPermisoInstalacionService grupoPermisoInstalacionService;

    @Autowired
    @Qualifier("UsuarioInstalacionService")
    private UsuarioInstalacionService usuarioInstalacionService;

    @Autowired
    private Environment env;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    public void init() throws IOException {
        if (permisoInstalacionService.count() == 0){
            permisoInstalacionService.findAll()
                    .forEach(mPermiso -> {
                        mPermiso.setFecha(new Date());
                        permisoInstalacionService.getRepository().save(mPermiso);
                    });
        }

        if (grupoInstalacionService.count() == 0){
            grupoInstalacionService.findAll()
                    .forEach(mGrupo -> {
                        mGrupo.setFecha(new Date());
                        grupoInstalacionService.getRepository().save(mGrupo);
                    });
        }

        if (grupoPermisoInstalacionService.count() == 0){
            grupoPermisoInstalacionService.findAll()
                    .forEach(mGrupoPermiso -> {
                        mGrupoPermiso.setFecha(new Date());
                        grupoPermisoInstalacionService.getRepository().save(mGrupoPermiso);
                    });
        }

        if (usuarioInstalacionService.count() == 0){
            usuarioInstalacionService.findAll()
                    .forEach(mUsuario -> {
                        mUsuario.setFecha(new Date());
                        mUsuario.setContrasena(passwordEncoder.encode(mUsuario.getContrasena()));
                        usuarioInstalacionService.getRepository().save(mUsuario);
                    });
        }
    }
}
