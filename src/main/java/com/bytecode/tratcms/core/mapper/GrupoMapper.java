package com.bytecode.tratcms.mapper;

import com.bytecode.tratcms.model.Grupo;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class GrupoMapper implements RowMapper<Grupo> {
    @Override
    public Grupo mapRow(ResultSet rs, int rowNum) throws SQLException {
        Grupo grupo = new Grupo();
        grupo.setIdgrupo(rs.getInt("IdGrupo"));
        grupo.setNombre(rs.getString("Nombre"));
        grupo.setFecha(rs.getDate("Fecha"));
        return grupo;
    }
}
