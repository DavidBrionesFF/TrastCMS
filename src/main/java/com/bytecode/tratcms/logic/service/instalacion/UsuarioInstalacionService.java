package com.bytecode.tratcms.logic.service.instalacion;

import com.bytecode.tratcms.data.model.MGrupo;
import com.bytecode.tratcms.data.model.MUsuario;
import com.bytecode.tratcms.data.repository.UsuarioRepository;
import com.bytecode.tratcms.data.repository.jpa.JpaUsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

@Service("UsuarioInstalacionService")
public class UsuarioInstalacionService implements InstalacionRegistroService<MUsuario, UsuarioRepository> {
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private JpaUsuarioRepository jpaUsuarioRepository;

    @Override
    public List<MUsuario> findAll() throws IOException {
        File file = ResourceUtils.getFile("classpath:data/usuario.json");
        String content = new String(Files.readAllBytes(file.toPath()));
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(content, mapper.getTypeFactory().constructCollectionType(List.class, MUsuario.class));
    }

    @Override
    public int count() {
        return (int) jpaUsuarioRepository.count();
    }

    @Override
    public UsuarioRepository getRepository() {
        return usuarioRepository;
    }
}
