package com.bytecode.tratcms.data.repository;

import com.bytecode.tratcms.data.model.MCategoria;

public interface CategoriaRep extends BaseRep<MCategoria> {
    public boolean deleteById(int id);
}
