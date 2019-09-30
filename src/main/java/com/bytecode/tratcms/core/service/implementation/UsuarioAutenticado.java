package com.bytecode.tratcms.core.service.implementation;

import com.bytecode.tratcms.core.model.common.UsuarioContext;
import com.bytecode.tratcms.core.repository.GrupoPermisoRep;
import com.bytecode.tratcms.core.repository.GrupoRep;
import com.bytecode.tratcms.core.repository.UsuarioRep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UsuarioAutenticado {
    @Autowired
    private UsuarioRep usuarioRep;

    @Autowired
    private GrupoPermisoRep grupoPermisoRep;

    @Autowired
    private GrupoRep grupoRep;

    public UsuarioContext getUsuarioContext(){
        UsuarioContext usuarioContext = UsuarioContext.create();

        return usuarioContext;
    }
}
