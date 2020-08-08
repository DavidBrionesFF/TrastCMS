package com.bytecode.tratcms.repository;

import com.bytecode.tratcms.model.MCategoria;

public interface CategoriaRep extends BaseRep<MCategoria> {
    public boolean deleteById(int id);
}
