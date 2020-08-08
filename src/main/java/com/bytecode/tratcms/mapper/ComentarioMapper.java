package com.bytecode.tratcms.mapper;

import com.bytecode.tratcms.model.MComentario;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ComentarioMapper implements RowMapper<MComentario> {
    @Override
    public MComentario mapRow(ResultSet rs, int rowNum) throws SQLException {
        MComentario MComentario = new MComentario();
        MComentario.setComentario(rs.getString("Comentario"));
        MComentario.setFecha(rs.getDate("Fecha"));
        MComentario.setIdComentario(rs.getInt("IdComentario"));
        MComentario.setIdPost(rs.getInt("IdPost"));
        MComentario.setIdUsuario(rs.getInt("IdUsuario"));
        MComentario.setRespuesta(rs.getString("Respuesta"));
        return MComentario;
    }
}
