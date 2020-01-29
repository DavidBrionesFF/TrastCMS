package com.bytecode.tratcms.repository;

import java.util.List;

import com.bytecode.tratcms.mapper.PostMetadataMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.bytecode.tratcms.model.PostMetadata;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

@Repository
public class PostMetadataRepository implements PostMetadataRep{
	@Autowired
	private DataSource dataSource;
	private JdbcTemplate jdbcTemplate;

	@PostConstruct
	public void postConstruct(){
		jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Override
	public boolean save(PostMetadata object) {
		try {
			String sql = String.format("insert into post_metadata (Clave, Valor, Tipo, IdPost) values ('%s', '%s', '%s', '%d')",
					object.getClave(), object.getValor(), object.getTipo(), object.getIdPost());
			jdbcTemplate.execute(sql);
			return true;
		}catch(Exception e) {
			return false;
		}
	}

	@Override
	public boolean update(PostMetadata object) {
		if(object.getIdPost()>0) {
			String sql = String.format("update post_metadata set Clave='%s', Valor='%s', Tipo='%s', IdPost='%d' where IdPostMetadata='%d'", 
						object.getClave(), object.getValor(), object.getTipo(), object.getIdPost(), object.getIdPostMetadata());
			jdbcTemplate.execute(sql);
		}
		return false;
	}

	@Override
	public List<PostMetadata> findAll(Pageable pageable) {
		return jdbcTemplate.query("select * from post_metadata", new PostMetadataMapper());
	}

	@Override
	public PostMetadata findById(int Id) {
		Object[] params = new Object[] {Id};
		return jdbcTemplate.queryForObject("select * from post_metadata where IdPostMetadata = ?",
				params, new PostMetadataMapper());
	}

	public JdbcTemplate getJdbcTemplate() {
		return jdbcTemplate;
	}

	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public List<PostMetadata> findByIdPost(int idPost) {
		Object[] params = new Object[] {idPost};
		return jdbcTemplate.query("select * from post_metadata where IdPost = ?",
				params, new PostMetadataMapper());
	}
}
