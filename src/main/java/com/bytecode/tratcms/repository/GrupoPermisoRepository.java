package com.bytecode.tratcms.repository;

import java.util.List;

import com.bytecode.tratcms.mapper.GrupoPermisoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;

import com.bytecode.tratcms.model.MGrupoPermiso;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

@Repository
public class GrupoPermisoRepository implements GrupoPermisoRep{
	@Autowired
	private DataSource dataSource;
	private JdbcTemplate jdbcTemplate;

	@PostConstruct
	public void postConstruct(){
		jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Override
	public boolean save(MGrupoPermiso object) {
		try {
			String sql = String.format("insert into grupo_permiso (IdGrupo, IdPermiso) values ('%d', '%d')",
					object.getIdGrupo(), object.getIdPermiso());
			jdbcTemplate.execute(sql);
			return true;
		}catch (Exception e) {
			return false;
		}
	}

	@Override
	public boolean update(MGrupoPermiso object) {
		if(object.getIdGrupo()>0) {
			String sql = String.format("update grupo_permiso set IdPermiso='%d' where IdGrupo='%d'",
					object.getIdPermiso(), object.getIdGrupo());
			jdbcTemplate.execute(sql);
		}
		return false;
	}

	@Override
	public List<MGrupoPermiso> findAll(Pageable pageable) {
		return jdbcTemplate.query("select * from grupo_permiso", new GrupoPermisoMapper());
	}

	@Override
	public MGrupoPermiso findById(int Id) {
		Object[] params = new Object[] {Id};
		return jdbcTemplate.queryForObject("select * from grupo_permiso where IdGrupoPermiso = ?",
				params, new GrupoPermisoMapper());
	}

	public JdbcTemplate getJdbcTemplate() {
		return jdbcTemplate;
	}

	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

}
