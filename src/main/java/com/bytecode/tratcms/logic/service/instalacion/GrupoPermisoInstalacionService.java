package com.bytecode.tratcms.logic.service.instalacion;

import com.bytecode.tratcms.data.model.MGrupoPermiso;
import com.bytecode.tratcms.data.model.MPermiso;
import com.bytecode.tratcms.data.repository.GrupoPermisoRepository;
import com.bytecode.tratcms.data.repository.GrupoRepository;
import com.bytecode.tratcms.data.repository.jpa.JpaGrupoPermisoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

@Service("GrupoPermisoInstalacionService")
public class GrupoPermisoInstalacionService implements InstalacionRegistroService<MGrupoPermiso, GrupoPermisoRepository> {
    @Autowired
    private GrupoPermisoRepository grupoPermisoRepository;
    @Autowired
    private JpaGrupoPermisoRepository jpaGrupoPermisoRepository;

    @Override
    public List<MGrupoPermiso> findAll() throws IOException {
        File file = ResourceUtils.getFile("classpath:data/grupo_permiso.json");
        String content = new String(Files.readAllBytes(file.toPath()));
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(content, mapper.getTypeFactory().constructCollectionType(List.class, MGrupoPermiso.class));
    }

    @Override
    public int count() {
        return (int) jpaGrupoPermisoRepository.count();
    }

    @Override
    public GrupoPermisoRepository getRepository() {
        return grupoPermisoRepository;
    }
}
