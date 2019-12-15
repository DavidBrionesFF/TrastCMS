package com.bytecode.tratcms.repository;

import com.bytecode.tratcms.model.Categoria;

public interface CategoriaRep extends BaseRep<Categoria> {
    public boolean deleteById(int id);
}
