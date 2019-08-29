package com.bytecode.tratcms.repository;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.bytecode.tratcms.model.UsuarioMetadata;

@Repository
public class UsuarioMetadataRepository implements UsuarioMetadaRep {

	@Autowired 
	private JdbcTemplate jdbcTemplate;

	@Override
	public boolean save(UsuarioMetadata object) {
		try {
			String sql = String.format("insert into usuario_metadata (IdUsuario, Clave, Valor, Tipo) values ('%d', '%s', '%s', '%s')", 
					                  object.getIdUsuario(), object.getClave(), object.getValor(), object.getTipo());
			jdbcTemplate.execute(sql);
			return true;
		}catch(Exception e) {
			return false;
		}
	}

	@Override
	public boolean update(UsuarioMetadata object) {
		if(object.getIdUsuarioMetadata()>0) {
			String sql = String.format("update set usuario_metadata IdUsuario='%d', Clave='%s', Valor='%s', Tipo='%s' where IdPostMetadata='%d'", 
					object.getIdUsuario(), object.getClave(), object.getValor(), object.getTipo(), object.getIdUsuarioMetadata());
			jdbcTemplate.execute(sql);
			return true;
		}
		return false;
	}

	@Override
	public List<UsuarioMetadata> findAll(Pageable pageable) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public UsuarioMetadata findById(int Id) {
		// TODO Auto-generated method stub
		return null;
	}
}
