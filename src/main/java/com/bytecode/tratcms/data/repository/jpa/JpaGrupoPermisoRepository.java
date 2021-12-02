package com.bytecode.tratcms.data.repository.jpa;

import com.bytecode.tratcms.data.model.entity.GrupoPermiso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.io.Serializable;

@Repository
public interface JpaGrupoPermisoRepository extends JpaRepository<GrupoPermiso, Serializable> {
}
