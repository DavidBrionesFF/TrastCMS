package com.bytecode.tratcms.data.repository;

import com.bytecode.tratcms.data.model.MPermiso;

import java.util.List;

public interface PermisoRep extends BaseRep<MPermiso> {
    public boolean deleteById(long idPermiso);

    public List<MPermiso> findByIdGrupo(int idGrupo);
    public List<MPermiso> findByNotIdGrupo(int idGrupo);
}
