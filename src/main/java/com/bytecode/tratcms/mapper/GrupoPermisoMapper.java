package com.bytecode.tratcms.mapper;

import com.bytecode.tratcms.model.MGrupoPermiso;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class GrupoPermisoMapper implements RowMapper<MGrupoPermiso> {
    @Override
    public MGrupoPermiso mapRow(ResultSet rs, int rowNum) throws SQLException {
        MGrupoPermiso MGrupoPermiso = new MGrupoPermiso();
        MGrupoPermiso.setIdGrupo(rs.getInt("IdGrupo"));
        MGrupoPermiso.setIdPermiso(rs.getInt("IdPermiso"));
        MGrupoPermiso.setFecha(rs.getDate("Fecha"));
        return MGrupoPermiso;
    }
}
