package com.bytecode.tratcms.repository;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.bytecode.tratcms.model.Comentario;

@Repository
public class ComentarioRepository implements ComentarioRep {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Override
	public boolean save(Comentario comentario) {
		try {
			String sql = String.format(
					"insert into Comentario (Comentario,IdPost,IdUsuario,Respuesta) "
					+ "values('%s', '%d', '%d', '%d')", 
					comentario.getComentario(), comentario.getIdPost(), comentario.getIdUsuario(), comentario.getRespuesta());
			jdbcTemplate.execute(sql);
			return true;
		}catch(Exception e) {
			return false;
		}
	}

	@Override
	public boolean update(Comentario comentario) {
		if(comentario.getIdComentario()>0) {
			String sql = String.format("update Comentario set Comentario='%s', IdPost='%d', IdUsuario='%d', Respuesta='%s' where IdComentario='%d'",
					comentario.getComentario(), comentario.getIdPost(), comentario.getIdUsuario(), comentario.getRespuesta(), comentario.getIdComentario());
			jdbcTemplate.execute(sql);
			return true;
		}
		return false;
	}

	@Override
	public List<Comentario> findAll(Pageable pageable) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Comentario findById(int Id) {
		// TODO Auto-generated method stub
		return null;
	}
}
