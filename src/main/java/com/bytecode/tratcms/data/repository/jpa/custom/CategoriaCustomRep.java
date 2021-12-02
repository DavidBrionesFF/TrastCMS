package com.bytecode.tratcms.data.repository.jpa.custom;

import com.bytecode.tratcms.data.model.entity.Categoria;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;

@NoRepositoryBean
public interface CategoriaCustomRep {
    public List<Categoria> findAll();
}
