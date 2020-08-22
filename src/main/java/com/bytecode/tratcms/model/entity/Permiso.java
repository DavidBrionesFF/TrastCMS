package com.bytecode.tratcms.model.entity;

import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "permiso")
public class Permiso implements Serializable {
    @Id
    @Column(name = "IdPermiso")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long IdPermiso;

    @Column(name = "Nombre")
    private String Nombre;

    @Column(name = "Fecha")
    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Date Fecha;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "Permiso")
    private List<GrupoPermiso> grupoPermisos;

    public long getIdPermiso() {
        return IdPermiso;
    }

    public void setIdPermiso(long idPermiso) {
        IdPermiso = idPermiso;
    }

    public String getNombre() {
        return Nombre;
    }

    public void setNombre(String nombre) {
        Nombre = nombre;
    }

    public Date getFecha() {
        return Fecha;
    }

    public void setFecha(Date fecha) {
        Fecha = fecha;
    }
}
