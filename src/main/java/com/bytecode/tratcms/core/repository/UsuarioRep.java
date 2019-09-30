package com.bytecode.tratcms.core.repository;

import com.bytecode.tratcms.core.model.Usuario;

public interface UsuarioRep extends BaseRep<Usuario> {

    Usuario findByCorreo(String correo);
}
