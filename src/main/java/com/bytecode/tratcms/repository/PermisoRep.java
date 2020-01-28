package com.bytecode.tratcms.repository;

import com.bytecode.tratcms.model.Permiso;

import java.util.List;

public interface PermisoRep extends BaseRep<Permiso> {
    public boolean deleteById(long idPermiso);

    public List<Permiso> findByIdGrupo(int idGrupo);
    public List<Permiso> findByNotIdGrupo(int idGrupo);
}
