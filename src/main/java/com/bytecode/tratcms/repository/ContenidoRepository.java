package com.bytecode.tratcms.repository;

import java.util.List;

import com.bytecode.tratcms.mapper.ContenidoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.bytecode.tratcms.model.Contenido;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

@Repository
public class ContenidoRepository implements ContenidoRep {
	@Autowired
	private DataSource dataSource;
	private JdbcTemplate jdbcTemplate;

	@PostConstruct
	public void postConstruct(){
		jdbcTemplate = new JdbcTemplate(dataSource);
	}

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
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public boolean update(Contenido object) {
		if(object.getIdContenido()>0) {
			String sql = String.format("update Contenido set Contenido='%s', Tipo='%s' where IdContenido='%d'",
					object.getContenido(), object.getTipo(), object.getIdContenido());
			jdbcTemplate.execute(sql);
			return true;
		}
		return false;
	}

	@Override
	public List<Contenido> findAll(Pageable pageable) {
		return jdbcTemplate.query("select * from contenido", new ContenidoMapper());
	}

	@Override
	public Contenido findById(int Id) {
		Object[] params = new Object[] {Id};
		return jdbcTemplate.queryForObject("select * from contenido where IdContenido = ?",
				params, new ContenidoMapper());
	}

	public JdbcTemplate getJdbcTemplate() {
		return jdbcTemplate;
	}

	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}
}
