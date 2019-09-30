package com.bytecode.tratcms.core.model.common;

import com.bytecode.tratcms.core.model.Permiso;
import com.bytecode.tratcms.core.model.Usuario;
import com.bytecode.tratcms.core.model.UsuarioMetadata;

import java.util.List;

public class UsuarioContext {
    private Usuario usuario;
    private List<Permiso> permisos;
    private List<UsuarioMetadata> usuarioMetadatas;

    public List<UsuarioMetadata> getUsuarioMetadatas() {
        return usuarioMetadatas;
    }

    public void setUsuarioMetadatas(List<UsuarioMetadata> usuarioMetadatas) {
        this.usuarioMetadatas = usuarioMetadatas;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public List<Permiso> getPermisos() {
        return permisos;
    }

    public void setPermisos(List<Permiso> permisos) {
        this.permisos = permisos;
    }

    public static UsuarioContext create(){
        return new UsuarioContext();
    }
}
