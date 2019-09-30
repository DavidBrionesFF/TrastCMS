package com.bytecode.tratcms.core.repository;

import java.util.List;

import com.bytecode.tratcms.core.mapper.PostMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.bytecode.tratcms.core.model.Post;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

@Repository
public class PostRepository implements PostRep{
	@Autowired
	private DataSource dataSource;
	private JdbcTemplate jdbcTemplate;

	@PostConstruct
	public void postConstruct(){
		jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Override
	public boolean save(Post object) {
		try {
			String sql = String.format("insert into Post (Titulo, Slug, Extracto, IdUsuario, IdCategoria, ImagenDestacada, Tipo) values ('%s','%s', '%s', '%d', '%d', '%s', '%s')",
					      object.getTitulo(), object.getSlug(), object.getExtracto(), object.getIdUsuario(), object.getCategoria(), object.getImagenDestacada(), object.getTipo());
			jdbcTemplate.execute(sql);
			return true;
		}catch(Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public boolean update(Post object) {
		if(object.getIdPost()>0) {
			String sql = String.format("update Post set Titulo='%s', Slug='%s', Extracto='%s', IdUsuario='%d', IdCategoria='%d', ImagenDestacada='%s', Tipo='%s' where IdPost=%d",
					object.getTitulo(), object.getSlug(), object.getExtracto(), object.getIdUsuario(), object.getCategoria(), object.getImagenDestacada(), object.getTipo(), object.getIdPost());
			
			jdbcTemplate.execute(sql);
			return true;
		}
		return false;
	}

	@Override
	public List<Post> findAll(Pageable pageable) {
		return jdbcTemplate.query("select * from post", new PostMapper());
	}

	@Override
	public Post findById(int Id) {
		Object[] params = new Object[] {Id};
		return jdbcTemplate.queryForObject("select * from post where IdPost = ?",
				params, new PostMapper());
	}

	@Override
	public long countAll() {
		return jdbcTemplate.queryForObject("select count(*) from post", Integer.class);
	}

	public JdbcTemplate getJdbcTemplate() {
		return jdbcTemplate;
	}

	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}
}
