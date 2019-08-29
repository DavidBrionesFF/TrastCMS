package com.bytecode.tratcms.repository;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;

import com.bytecode.tratcms.model.GrupoPermiso;

public class GrupoPermisoRepository implements GrupoPermisoRep{
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Override
	public boolean save(GrupoPermiso object) {
		try {
			String sql = String.format("insert into GrupoPermiso (IdGrupo, IdPermiso) values ('%d', '%d')", 
					object.getIdGrupo(), object.getIdPermiso());
			jdbcTemplate.execute(sql);
			return true;
		}catch (Exception e) {
			return false;
		}
	}

	@Override
	public boolean update(GrupoPermiso object) {
		if(object.getIdGrupo()>0) {
			String sql = String.format("update Grupo set IdPermiso='%d' where IdGrupo='%d'",
					object.getIdPermiso(), object.getIdGrupo());
			jdbcTemplate.execute(sql);
		}
		return false;
	}

	@Override
	public List<GrupoPermiso> findAll(Pageable pageable) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public GrupoPermiso findById(int Id) {
		// TODO Auto-generated method stub
		return null;
	}

	

}
