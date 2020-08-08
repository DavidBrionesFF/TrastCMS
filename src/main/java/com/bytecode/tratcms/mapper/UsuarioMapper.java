package com.bytecode.tratcms.mapper;

import com.bytecode.tratcms.model.MUsuario;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class UsuarioMapper implements RowMapper<MUsuario> {
    @Override
    public MUsuario mapRow(ResultSet rs, int rowNum) throws SQLException {
        MUsuario MUsuario = new MUsuario();
        MUsuario.setApellido(rs.getString("Apellido"));
        MUsuario.setContrasena(rs.getString("Contrasena"));
        MUsuario.setCorreo(rs.getString("Correo"));
        MUsuario.setIdGrupo(rs.getInt("IdGrupo"));
        MUsuario.setIdUsuario(rs.getInt("IdUsuario"));
        MUsuario.setNombre(rs.getString("Nombre"));
        MUsuario.setFecha(rs.getDate("Fecha"));
        return MUsuario;
    }
}
