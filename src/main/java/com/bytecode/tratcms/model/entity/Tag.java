package com.bytecode.tratcms.model.entity;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "tag")
public class Tag implements Serializable {
    @Id
    @Column(name = "IdTag")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int IdTag;

    @Column(name = "Nombre")
    private String nombre;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "IdCategoria")
    private Categoria categoria;

    public int getIdTag() {
        return IdTag;
    }

    public void setIdTag(int idTag) {
        IdTag = idTag;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }
}
