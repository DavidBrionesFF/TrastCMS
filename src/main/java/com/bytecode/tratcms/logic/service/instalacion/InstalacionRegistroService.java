package com.bytecode.tratcms.logic.service.instalacion;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

public interface InstalacionRegistroService<T, K> {
    public List<T> findAll() throws IOException;
    public int count();
    public K getRepository();
}
