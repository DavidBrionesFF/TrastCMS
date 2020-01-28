package com.bytecode.tratcms.repository;

import java.util.List;

import com.bytecode.tratcms.mapper.PermisoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.bytecode.tratcms.model.Permiso;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

@Repository
public class PermisoRepository implements PermisoRep {
	@Autowired
	private DataSource dataSource;
	private JdbcTemplate jdbcTemplate;

	@PostConstruct
	public void postConstruct(){
		jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Override
	public boolean save(Permiso object) {
		try {
			String sql = String.format("insert into Permiso (Nombre) values ('%s')", 
					object.getNombre());
			jdbcTemplate.execute(sql);
			return true;
		}catch(Exception e) {
			return false;
		}
	}

	@Override
	public boolean update(Permiso object) {
		if(object.getIdPermiso()>0) {
			String sql = String.format("update Permiso set Nombre='%s' where IdPermiso='%d'", 
					object.getNombre(), object.getIdPermiso());
			jdbcTemplate.execute(sql);
			return true;
		}
		return false;
	}

	@Override
	public List<Permiso> findAll(Pageable pageable) {
		return jdbcTemplate.query("select * from permiso", new PermisoMapper());
	}

	@Override
	public Permiso findById(int Id) {
		Object[] params = new Object[] {Id};
		return jdbcTemplate.queryForObject("select * from permiso where IdPermiso = ?",
				params, new PermisoMapper());
	}

	public JdbcTemplate getJdbcTemplate() {
		return jdbcTemplate;
	}

	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public boolean deleteById(long idPermiso) {
		if(idPermiso>0) {
			String sql = String.format("delete from permiso where IdPermiso='%d'", idPermiso);
			jdbcTemplate.execute(sql);
			return true;
		}
		return false;
	}

	@Override
	public List<Permiso> findByIdGrupo(int idGrupo) {
		Object[] params = new Object[] {idGrupo};
		return jdbcTemplate.query("select p.* from grupo_permiso gp inner join permiso p on gp.IdPermiso = p.IdPermiso where gp.IdGrupo=?",
				params,
				new PermisoMapper());
	}

	@Override
	public List<Permiso> findByNotIdGrupo(int idGrupo) {
		Object[] params = new Object[] {idGrupo};
		return jdbcTemplate.query("select p.* from grupo_permiso gp inner join permiso p on gp.IdPermiso = p.IdPermiso where gp.IdGrupo!=?",
				params,
				new PermisoMapper());
	}
}
