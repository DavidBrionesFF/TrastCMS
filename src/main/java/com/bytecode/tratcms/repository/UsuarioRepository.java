package com.bytecode.tratcms.repository;

import java.util.List;

import com.bytecode.tratcms.mapper.UsuarioMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.bytecode.tratcms.model.Usuario;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

@Repository
public class UsuarioRepository implements UsuarioRep {
	@Autowired
	private DataSource dataSource;
	private JdbcTemplate jdbcTemplate;

	@PostConstruct
	public void postConstruct(){
		jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Override
	public boolean save(Usuario object) {
		try {
			String sql = String.format("insert into Usuario (Nombre, Apellido, Contrasena, Correo, IdGrupo) values ('%s', '%s', '%s', '%s', '%d')", 
					                  object.getNombre(), object.getApellido(), object.getContrasena(), object.getCorreo(), object.getIdGrupo());
			jdbcTemplate.execute(sql);
			return true;
		}catch(Exception e) {
			return false;
		}
	}

	@Override
	public boolean update(Usuario object) {
		if(object.getIdUsuario()>0) {
			String sql = String.format("update Usuario set Nombre='%s', Apellido='%s', Contrasena='%s', Correo='%s', IdGrupo='%d' where IdUsuario='%d'", 
					                  object.getNombre(), object.getApellido(), object.getContrasena(), object.getCorreo(), object.getIdGrupo(), object.getIdUsuario());
			jdbcTemplate.execute(sql);
			return true;
		}
		return false;
	}

	@Override
	public List<Usuario> findAll(Pageable pageable) {
		return jdbcTemplate.query("select * from Usuario", new UsuarioMapper());
	}

	@Override
	public Usuario findById(int Id) {
		Object[] params = new Object[] {Id};
		return jdbcTemplate.queryForObject("select * from Usuario where IdUsuario = ?",
												params, new UsuarioMapper());
	}

	public JdbcTemplate getJdbcTemplate() {
		return jdbcTemplate;
	}

	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public Usuario findByCorreo(String correo) {
		Object[] params = new Object[] {correo};
		return jdbcTemplate.queryForObject("select * from Usuario where Correo = ?",
				params, new UsuarioMapper());
	}
}
