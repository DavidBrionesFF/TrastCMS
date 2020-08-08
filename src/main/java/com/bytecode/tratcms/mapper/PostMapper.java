package com.bytecode.tratcms.mapper;

import com.bytecode.tratcms.model.MPost;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PostMapper implements RowMapper<MPost> {
    @Override
    public MPost mapRow(ResultSet rs, int rowNum) throws SQLException {
        MPost MPost = new MPost();
        MPost.setCategoria(rs.getInt("IdCategoria"));
        MPost.setExtracto(rs.getString("Extracto"));
        MPost.setTitulo(rs.getString("Titulo"));
        MPost.setSlug(rs.getString("Slug"));
        MPost.setIdUsuario(rs.getInt("IdUsuario"));
        MPost.setIdPost(rs.getInt("IdPost"));
        MPost.setTipo(rs.getString("Tipo"));
        MPost.setImagenDestacada(rs.getString("ImagenDestacada"));
        MPost.setFecha(rs.getDate("Fecha"));
        return MPost;
    }
}
