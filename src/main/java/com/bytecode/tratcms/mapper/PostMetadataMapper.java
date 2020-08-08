package com.bytecode.tratcms.mapper;

import com.bytecode.tratcms.model.MPostMetadata;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PostMetadataMapper implements RowMapper<MPostMetadata> {
    @Override
    public MPostMetadata mapRow(ResultSet rs, int rowNum) throws SQLException {
        MPostMetadata MPostMetadata = new MPostMetadata();
        MPostMetadata.setClave(rs.getString("Clave"));
        MPostMetadata.setIdPost(rs.getInt("IdPost"));
        MPostMetadata.setIdPostMetadata(rs.getInt("IdPostMetadata"));
        MPostMetadata.setTipo(rs.getString("Tipo"));
        MPostMetadata.setValor(rs.getString("Valor"));
        MPostMetadata.setFecha(rs.getDate("Fecha"));
        return MPostMetadata;
    }
}
