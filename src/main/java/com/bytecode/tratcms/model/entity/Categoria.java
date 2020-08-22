package com.bytecode.tratcms.model.entity;

import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "categoria")
public class Categoria implements Serializable {
    @Id
    @Column(name = "IdCategoria")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long IdCategoria;

    @Column(name = "Nombre")
    private String Nombre;

    @Column(name = "Descripcion")
    private String Descripcion;

    @Column(name = "Fecha")
    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Date Fecha = new Date();

    @OneToOne(optional = true, fetch = FetchType.EAGER, cascade = CascadeType.ALL, mappedBy = "categoria")
    private Tag tag;

    public long getIdCategoria() {
        return IdCategoria;
    }

    public void setIdCategoria(long idCategoria) {
        IdCategoria = idCategoria;
    }

    public String getNombre() {
        return Nombre;
    }

    public void setNombre(String nombre) {
        Nombre = nombre;
    }

    public String getDescripcion() {
        return Descripcion;
    }

    public void setDescripcion(String descripcion) {
        Descripcion = descripcion;
    }

    public Date getFecha() {
        return Fecha;
    }

    public void setFecha(Date fecha) {
        Fecha = fecha;
    }

    public Tag getTag() {
        return tag;
    }

    public void setTag(Tag tag) {
        this.tag = tag;
    }
}
