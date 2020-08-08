package com.bytecode.tratcms.mapper;

import com.bytecode.tratcms.model.MContenido;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ContenidoMapper implements RowMapper<MContenido> {
    @Override
    public MContenido mapRow(ResultSet rs, int rowNum) throws SQLException {
        MContenido MContenido = new MContenido();
        MContenido.setContenido(rs.getString("Contenido"));
        MContenido.setIdContenido(rs.getInt("IdContenido"));
        MContenido.setIdPost(rs.getInt("IdPost"));
        MContenido.setTipo(rs.getString("Tipo"));
        MContenido.setFecha(rs.getDate("Fecha"));
        return MContenido;
    }
}
