package com.bytecode.tratcms.core.service;

import com.bytecode.tratcms.core.model.Grupo;
import com.bytecode.tratcms.core.model.GrupoPermiso;
import com.bytecode.tratcms.core.model.Permiso;
import com.bytecode.tratcms.core.model.Usuario;

public interface IAgregarConfiguracionesService {
    //Runner
    public void addDefaultPermisos();
    public void addDefaultGrupos();
    public void addDefaultGrupoPermisos();
    public void addDefultUsuario();

    //Construct
    public Permiso constructPermiso(String Nombre);
    public Grupo constructGrupo(String Nombre);
    public GrupoPermiso constructGrupoPermiso(long IdGrupo, long IdPermiso);
    public Usuario constructUsuario(String Nombre, String Apellido, String Constrasena, String Correo, long idGrupo);
}
