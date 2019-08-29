package com.bytecode.tratcms.repository;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.bytecode.tratcms.model.Grupo;

@Repository
public class GrupoRepository implements GrupoRep{
	
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Override
	public boolean save(Grupo object) {
		try {
			String sql = String.format("insert into Grupo (Nombre) values ('%s')", object.getNombre());
			jdbcTemplate.execute(sql);
			return true;
		}catch (Exception e) {
			return false;
		}
	}

	@Override
	public boolean update(Grupo object) {
		if(object.getIdgrupo()>0) {
			String sql = String.format("update Grupo set Nombre='%s' where IdGrupo='%d'", object.getNombre(), object.getIdgrupo());
			jdbcTemplate.execute(sql);
			return true;
		}
		return false;
	}

	@Override
	public List<Grupo> findAll(Pageable pageable) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Grupo findById(int Id) {
		// TODO Auto-generated method stub
		return null;
	}
}
