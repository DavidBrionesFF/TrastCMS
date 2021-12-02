package com.bytecode.tratcms.data.repository.jpa;

import com.bytecode.tratcms.data.model.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.io.Serializable;

@Repository
public interface JpaTagRepository extends JpaRepository<Tag, Serializable> {
}
