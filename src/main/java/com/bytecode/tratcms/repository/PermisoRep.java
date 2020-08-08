package com.bytecode.tratcms.repository;

import com.bytecode.tratcms.model.MPermiso;

import java.util.List;

public interface PermisoRep extends BaseRep<MPermiso> {
    public boolean deleteById(long idPermiso);

    public List<MPermiso> findByIdGrupo(int idGrupo);
    public List<MPermiso> findByNotIdGrupo(int idGrupo);
}
