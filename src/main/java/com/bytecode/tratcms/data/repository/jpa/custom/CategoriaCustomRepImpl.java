package com.bytecode.tratcms.data.repository.jpa.custom;

import com.bytecode.tratcms.data.model.entity.Categoria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;

@Repository
public class CategoriaCustomRepImpl implements CategoriaCustomRep {
    @Autowired
    private EntityManager entityManager;

    @Override
    public List<Categoria> findAll() {
        String jpql = "select c from Categoria c";
        Query query = entityManager.createQuery(jpql);
        return query.getResultList();
    }
}
