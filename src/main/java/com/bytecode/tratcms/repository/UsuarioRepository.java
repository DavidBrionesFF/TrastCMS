package com.bytecode.tratcms.repository;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.bytecode.tratcms.model.Usuario;

@Repository
public class UsuarioRepository implements UsuarioRep {

	@Autowired
	private JdbcTemplate jdbcTemplate;

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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Usuario findById(int Id) {
		// TODO Auto-generated method stub
		return null;
	}
}
