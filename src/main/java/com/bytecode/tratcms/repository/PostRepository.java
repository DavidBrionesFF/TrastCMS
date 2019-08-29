package com.bytecode.tratcms.repository;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.bytecode.tratcms.model.Post;

@Repository
public class PostRepository implements PostRep{

	@Autowired 
	private JdbcTemplate jdbcTemplate;

	@Override
	public boolean save(Post object) {
		try {
			String sql = String.format("insert into Post (Titulo, Slug, Extracto, Idusuario, Categoria, ImagenDestacada, Tipo) values ('%s','%s', '%s', '%d', '%d', '%s', '%s')", 
					      object.getTitulo(), object.getSlug(), object.getExtracto(), object.getIdUsuario(), object.getCategoria(), object.getImagenDestacada(), object.getTipo());
			jdbcTemplate.execute(sql);
			return true;
		}catch(Exception e) {
			return false;
		}
	}

	@Override
	public boolean update(Post object) {
		if(object.getIdPost()>0) {
			String sql = String.format("update Post set Titutlo='%s', Slug='%s', Extracto='%s', Idusuario='%d', Categoria='%d', ImagenDestacada='%s', Tipo='%s'", 
					object.getTitulo(), object.getSlug(), object.getExtracto(), object.getIdUsuario(), object.getCategoria(), object.getImagenDestacada(), object.getTipo());
			
			jdbcTemplate.execute(sql);
			return true;
		}
		return false;
	}

	@Override
	public List<Post> findAll(Pageable pageable) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Post findById(int Id) {
		// TODO Auto-generated method stub
		return null;
	}
}
