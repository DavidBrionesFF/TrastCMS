package com.bytecode.tratcms.repository;

import java.util.List;

import com.bytecode.tratcms.mapper.CategoriaMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.bytecode.tratcms.model.Categoria;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

@Repository
public class CategoriaRepository implements CategoriaRep {
	private Log logger = LogFactory.getLog(getClass());
	@Autowired
	private DataSource dataSource;
	private JdbcTemplate jdbcTemplate;

	@PostConstruct
	public void postConstruct(){
		jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Override
	public boolean save(Categoria categoria) {
		try {
			String sql = String.format(
					"insert into Categoria (Nombre,Descripcion,CategoriaSuperior) "
					+ "values('%s', '%s', %d)",
					categoria.getNombre(), categoria.getDescripcion(), categoria.getCategoriaSuperior());
			jdbcTemplate.execute(sql);
			return true;
		}catch(Exception e) {
			e.printStackTrace();
			logger.error(e);
			return false;
		}
	}

	@Override
	public boolean update(Categoria categoria) {
		if(categoria.getIdCategoria() > 0) {
			String sql = String.format("update Categoria set Nombre='%s', Descripcion='%s', CategoriaSuperior='%d' "
					+ "where IdCategoria='%d'", 
					categoria.getNombre(), categoria.getDescripcion(), categoria.getCategoriaSuperior(),
					categoria.getIdCategoria());
			jdbcTemplate.execute(sql);
			return true;
		}
		return false;
	}

	@Override
	public List<Categoria> findAll(Pageable pageable) {
		return jdbcTemplate.query("select * from Categoria ", new CategoriaMapper());
	}

	@Override
	public Categoria findById(int Id) {
		Object[] params = new Object[] {Id};
		return jdbcTemplate.queryForObject("select * from Categoria where IdCategoria = ?", params, new CategoriaMapper());
	}

	public void deleteAll(){
		jdbcTemplate.execute("delete from Categoria");
	}

	public boolean deleteById(int id){
		try{
			String sql = String.format("delete from Categoria where IdCategoria='%d'", id);
			jdbcTemplate.execute(sql);
			return true;
		}catch (Exception e){
			logger.error(e);
			return false;
		}
	}

	public JdbcTemplate getJdbcTemplate() {
		return jdbcTemplate;
	}

	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}
}
