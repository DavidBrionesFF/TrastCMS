package com.bytecode.tratcms.repository.jpa;

import com.bytecode.tratcms.model.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

@Repository
public interface JpaCategoriaRepository extends JpaRepository<Categoria, Serializable> {
    // Si es una unica entidad, utilizar Optional
    // Si son muchas entidades utilizar List
    public Optional<Categoria>  findByNombre(String nombre);

    public List<Categoria> findByNombreLike(String nombre);
}
