package com.bytecode.tratcms.mapper;

import com.bytecode.tratcms.model.Post;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PostMapper implements RowMapper<Post> {
    @Override
    public Post mapRow(ResultSet rs, int rowNum) throws SQLException {
        Post post = new Post();
        post.setCategoria(rs.getInt("IdCategoria"));
        post.setExtracto(rs.getString("Extracto"));
        post.setTitulo(rs.getString("Titulo"));
        post.setSlug(rs.getString("Slug"));
        post.setIdUsuario(rs.getInt("IdUsuario"));
        post.setIdPost(rs.getInt("IdPost"));
        post.setTipo(rs.getString("Tipo"));
        post.setImagenDestacada(rs.getString("ImagenDestacada"));
        post.setFecha(rs.getDate("Fecha"));
        return post;
    }
}
