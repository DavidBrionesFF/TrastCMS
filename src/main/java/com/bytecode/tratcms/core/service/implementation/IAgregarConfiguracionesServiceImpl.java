package com.bytecode.tratcms.core.service.implementation;

import com.bytecode.tratcms.core.model.Grupo;
import com.bytecode.tratcms.core.model.GrupoPermiso;
import com.bytecode.tratcms.core.model.Permiso;
import com.bytecode.tratcms.core.model.Usuario;
import com.bytecode.tratcms.core.repository.PermisoRep;
import com.bytecode.tratcms.core.service.IAgregarConfiguracionesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class IAgregarConfiguracionesServiceImpl implements IAgregarConfiguracionesService {
    @Autowired
    private PermisoRep permisoRep;

    @Override
    public void addDefaultPermisos() {
        if (permisoRep.countAll() == 0){

        }
    }

    @Override
    public void addDefaultGrupos() {

    }

    @Override
    public void addDefaultGrupoPermisos() {

    }

    @Override
    public void addDefultUsuario() {

    }

    @Override
    public Permiso constructPermiso(String Nombre) {
        return null;
    }

    @Override
    public Grupo constructGrupo(String Nombre) {
        return null;
    }

    @Override
    public GrupoPermiso constructGrupoPermiso(long IdGrupo, long IdPermiso) {
        return null;
    }

    @Override
    public Usuario constructUsuario(String Nombre, String Apellido, String Constrasena, String Correo, long idGrupo) {
        return null;
    }
}
