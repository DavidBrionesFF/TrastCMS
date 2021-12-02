package com.bytecode.tratcms.data.repository;

import com.bytecode.tratcms.data.model.MUsuario;

public interface UsuarioRep extends BaseRep<MUsuario> {
    public MUsuario findByCorreo(String correo);
}
