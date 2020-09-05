package com.bytecode.tratcms.model.entity;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "post")
public class Post implements Serializable {
    @Id
    @Column(name = "IdPost")
    private long IdPost;

    @Column(name = "Titulo")
    private String Titulo;

    @Column(name = "Slug")
    private String Slug;

    @Column(name = "Extracto")
    private String Extracto;

    @Column(name = "ImagenDestacada")
    private String ImagenDestacada;

    @Column(name = "Tipo")
    private String Tipo = "POST";

    @Column(name = "Fecha")
    private Date Fecha;

    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Usuario> usuarios;

    public long getIdPost() {
        return IdPost;
    }

    public void setIdPost(long idPost) {
        IdPost = idPost;
    }

    public String getTitulo() {
        return Titulo;
    }

    public void setTitulo(String titulo) {
        Titulo = titulo;
    }

    public String getSlug() {
        return Slug;
    }

    public void setSlug(String slug) {
        Slug = slug;
    }

    public String getExtracto() {
        return Extracto;
    }

    public void setExtracto(String extracto) {
        Extracto = extracto;
    }

    public String getImagenDestacada() {
        return ImagenDestacada;
    }

    public void setImagenDestacada(String imagenDestacada) {
        ImagenDestacada = imagenDestacada;
    }

    public String getTipo() {
        return Tipo;
    }

    public void setTipo(String tipo) {
        Tipo = tipo;
    }

    public Date getFecha() {
        return Fecha;
    }

    public void setFecha(Date fecha) {
        Fecha = fecha;
    }

    public List<Usuario> getUsuarios() {
        return usuarios;
    }

    public void setUsuarios(List<Usuario> usuarios) {
        this.usuarios = usuarios;
    }
}
