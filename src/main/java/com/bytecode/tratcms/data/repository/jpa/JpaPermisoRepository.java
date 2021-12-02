package com.bytecode.tratcms.data.repository.jpa;

import com.bytecode.tratcms.data.model.entity.Permiso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.io.Serializable;

@Repository
public interface JpaPermisoRepository extends JpaRepository<Permiso, Serializable> {
}
