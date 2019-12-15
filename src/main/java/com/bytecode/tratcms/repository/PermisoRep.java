package com.bytecode.tratcms.repository;

import com.bytecode.tratcms.model.Permiso;

public interface PermisoRep extends BaseRep<Permiso> {
    public boolean deleteById(long idPermiso);
}
