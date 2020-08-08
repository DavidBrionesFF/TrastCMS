package com.bytecode.tratcms.mapper;

import com.bytecode.tratcms.model.MGrupo;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class GrupoMapper implements RowMapper<MGrupo> {
    @Override
    public MGrupo mapRow(ResultSet rs, int rowNum) throws SQLException {
        MGrupo MGrupo = new MGrupo();
        MGrupo.setIdgrupo(rs.getInt("IdGrupo"));
        MGrupo.setNombre(rs.getString("Nombre"));
        MGrupo.setFecha(rs.getDate("Fecha"));
        return MGrupo;
    }
}
