package com.bytecode.tratcms.data.repository.jpa;

import com.bytecode.tratcms.data.model.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.io.Serializable;
import java.util.List;

@Repository
public interface JpaPostRepository extends JpaRepository<Post, Serializable> {

    @Query("SELECT COUNT(p) FROM Post p")
    public long count();

    @Query("SELECT COUNT(p) FROM Post p WHERE p.idUsuario.idUsuario = :idUsuario")
    public long countByUsuario(@Param("idUsuario") int idUsuario);

    @Query("SELECT COUNT(p) FROM Post p WHERE p.idCategoria.idCategoria = :idCategoria")
    public long countByCategoria(@Param("idCategoria") int idCategoria);

    @Query("SELECT p FROM Post p ORDER BY p.fecha DESC")
    public List<Post> findTop10ByOrderByFechaDesc();

    @Query("SELECT p FROM Post p ORDER BY SIZE(p.comentarioList) DESC")
    public List<Post> findTop10OrderByComentarioListDesc();

    @Query("SELECT p FROM Post p WHERE p.idCategoria.idCategoria = :idCategoria ORDER BY SIZE(p.comentarioList) DESC")
    public List<Post> findTop10ByCategoriaOrderByComentarioListDesc(@Param("idCategoria") int idCategoria);

    @Query("SELECT p FROM Post p WHERE p.idUsuario.idUsuario = :idUsuario ORDER BY SIZE(p.comentarioList) DESC")
    public List<Post> findTop10ByUsuarioOrderByComentarioListDesc(@Param("idUsuario") int idUsuario);
}
