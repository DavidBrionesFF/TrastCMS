package com.bytecode.tratcms.mapper;

import com.bytecode.tratcms.model.MPermiso;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PermisoMapper implements RowMapper<MPermiso> {
    @Override
    public MPermiso mapRow(ResultSet rs, int rowNum) throws SQLException {
        MPermiso MPermiso = new MPermiso();
        MPermiso.setIdPermiso(rs.getInt("IdPermiso"));
        MPermiso.setNombre(rs.getString("Nombre"));
        MPermiso.setFecha(rs.getDate("Fecha"));
        return MPermiso;
    }
}
