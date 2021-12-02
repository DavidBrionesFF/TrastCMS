package com.bytecode.tratcms.logic.service.instalacion;

import com.bytecode.tratcms.data.model.MGrupo;
import com.bytecode.tratcms.data.model.MGrupoPermiso;
import com.bytecode.tratcms.data.repository.GrupoRepository;
import com.bytecode.tratcms.data.repository.jpa.JpaGrupoPermisoRepository;
import com.bytecode.tratcms.data.repository.jpa.JpaGrupoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

@Service("GrupoInstalacionService")
public class GrupoInstalacionService implements InstalacionRegistroService<MGrupo, GrupoRepository> {
    @Autowired
    private GrupoRepository grupoRepository;
    @Autowired
    private JpaGrupoRepository jpaGrupoRepository;

    @Override
    public List<MGrupo> findAll() throws IOException {
        File file = ResourceUtils.getFile("classpath:data/grupo.json");
        String content = new String(Files.readAllBytes(file.toPath()));
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(content, mapper.getTypeFactory().constructCollectionType(List.class, MGrupo.class));
    }

    @Override
    public int count() {
        return (int) jpaGrupoRepository.count();
    }

    @Override
    public GrupoRepository getRepository() {
        return grupoRepository;
    }
}
