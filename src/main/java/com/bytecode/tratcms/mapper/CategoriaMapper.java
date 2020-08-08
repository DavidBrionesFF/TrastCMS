package com.bytecode.tratcms.mapper;

import com.bytecode.tratcms.model.MCategoria;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class CategoriaMapper implements RowMapper<MCategoria> {
    @Override
    public MCategoria mapRow(ResultSet rs, int rowNum) throws SQLException {
        MCategoria MCategoria = new MCategoria();
        MCategoria.setCategoriaSuperior(rs.getInt("CategoriaSuperior"));
        MCategoria.setDescripcion(rs.getString("Descripcion"));
        MCategoria.setFecha(rs.getTimestamp("Fecha"));
        MCategoria.setIdCategoria(rs.getInt("IdCategoria"));
        MCategoria.setNombre(rs.getString("Nombre"));

        return MCategoria;
    }
}
