package com.bytecode.tratcms.repository;

import com.bytecode.tratcms.model.MUsuario;

public interface UsuarioRep extends BaseRep<MUsuario> {
    public MUsuario findByCorreo(String correo);
}
