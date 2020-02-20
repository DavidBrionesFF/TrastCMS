package com.bytecode.tratcms.repository;

import com.bytecode.tratcms.model.Usuario;

public interface UsuarioRep extends BaseRep<Usuario> {
    public Usuario findByCorreo(String correo);
}
