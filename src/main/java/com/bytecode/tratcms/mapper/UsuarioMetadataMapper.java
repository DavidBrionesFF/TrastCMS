package com.bytecode.tratcms.mapper;

import com.bytecode.tratcms.model.MUsuarioMetadata;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class UsuarioMetadataMapper implements RowMapper<MUsuarioMetadata> {
    @Override
    public MUsuarioMetadata mapRow(ResultSet rs, int rowNum) throws SQLException {
        MUsuarioMetadata MUsuarioMetadata = new MUsuarioMetadata();
        MUsuarioMetadata.setClave(rs.getString("Clave"));
        MUsuarioMetadata.setIdUsuario(rs.getInt("IdUsuario"));
        MUsuarioMetadata.setIdUsuarioMetadata(rs.getInt("IdUsuarioMetadata"));
        MUsuarioMetadata.setValor(rs.getString("Valor"));
        MUsuarioMetadata.setTipo(rs.getString("Tipo"));
        MUsuarioMetadata.setFecha(rs.getDate("Fecha"));
        return MUsuarioMetadata;
    }
}
