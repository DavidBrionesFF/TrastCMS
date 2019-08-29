package com.bytecode.tratcms.repository;

import java.util.List;

import com.bytecode.tratcms.mapper.CategoriaMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.bytecode.tratcms.model.Categoria;

@Repository
public class CategoriaRepository implements CategoriaRep {
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Override
	public boolean save(Categoria categoria) {
		try {
			String sql = String.format(
					"insert into Categoria (Nombre,Descripcion,CategoriaSuperior) "
					+ "values('%s', '%s', '%d')", 
					categoria.getNombre(), categoria.getDescripcion(), categoria.getCategoriaSuperiorior());
			jdbcTemplate.execute(sql);
			return true;
		}catch(Exception e) {
			return false;
		}
	}

	@Override
	public boolean update(Categoria categoria) {
		if(categoria.getIdCategoria() > 0) {
			String sql = String.format("update Categoria set Nombre='%s', Descripcion='%s', CategoriaSuperior='%d' "
					+ "where IdCategoria='%d'", 
					categoria.getNombre(), categoria.getDescripcion(), categoria.getCategoriaSuperiorior(),
					categoria.getIdCategoria());
			jdbcTemplate.execute(sql);
			return true;
		}
		return false;
	}

	@Override
	public List<Categoria> findAll(Pageable pageable) {
		return jdbcTemplate.query("select * from Categoria", new CategoriaMapper());
	}

	@Override
	public Categoria findById(int Id) {
		Object[] params = new Object[] {Id};
		return jdbcTemplate.queryForObject("select * from Categoria where IdCategoria = ?", params, new CategoriaMapper());
	}
}
