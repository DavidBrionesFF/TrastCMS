package com.bytecode.tratcms.logic.service.instalacion;

import com.bytecode.tratcms.data.model.MPermiso;
import com.bytecode.tratcms.data.repository.PermisoRepository;
import com.bytecode.tratcms.data.repository.jpa.JpaPermisoRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

@Service("PermisoInstalacionService")
public class PermisoInstalacionService implements InstalacionRegistroService<MPermiso, PermisoRepository>{
    @Autowired
    private PermisoRepository permisoRepository;
    @Autowired
    private JpaPermisoRepository jpaPermisoRepository;

    @Override
    public List<MPermiso> findAll() throws IOException {
        File file = ResourceUtils.getFile("classpath:data/permiso.json");
        String content = new String(Files.readAllBytes(file.toPath()));
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(content, mapper.getTypeFactory().constructCollectionType(List.class, MPermiso.class));
    }

    @Override
    public int count() {
        return (int) jpaPermisoRepository.count();
    }

    @Override
    public PermisoRepository getRepository() {
        return permisoRepository;
    }
}
