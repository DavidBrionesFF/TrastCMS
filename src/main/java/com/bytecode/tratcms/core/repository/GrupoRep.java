package com.bytecode.tratcms.core.repository;

import com.bytecode.tratcms.core.model.Grupo;

import java.util.List;

public interface GrupoRep extends BaseRep<Grupo> {
    public List<Grupo> findGrupoByUsuario(long id);
}
