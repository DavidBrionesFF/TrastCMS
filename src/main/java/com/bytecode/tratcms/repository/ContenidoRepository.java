package com.bytecode.tratcms.repository;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.bytecode.tratcms.model.Contenido;

@Repository
public class ContenidoRepository implements ContenidoRep {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Override
	public boolean save(Contenido object) {
		try {
			String sql = String.format(
					"insert into Contenido (Contenido,IdPost,Tipo) "
					+ "values('%s', '%d', '%s')", 
					object.getContenido(), object.getIdPost(), object.getTipo());
			jdbcTemplate.execute(sql);
			return true;
		}catch(Exception e) {
			return false;
		}
	}

	@Override
	public boolean update(Contenido object) {
		if(object.getIdContenido()>0) {
			String sql = String.format("update Contenido set Contenido='%s', Tipo='%s' where IdContenido='%d'",
					object.getContenido(), object.getTipo(), object.getIdContenido());
			jdbcTemplate.execute(sql);
		}
		return false;
	}

	@Override
	public List<Contenido> findAll(Pageable pageable) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Contenido findById(int Id) {
		// TODO Auto-generated method stub
		return null;
	}
}
