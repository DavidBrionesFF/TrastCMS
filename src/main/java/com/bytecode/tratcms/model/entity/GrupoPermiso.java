package com.bytecode.tratcms.model.entity;

import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "grupo_permiso")
public class GrupoPermiso implements Serializable {
    @Id
    @Column(name = "IdGrupoPermiso")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int IdGrupoPermiso;

    @Column(name = "Fecha")
    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Date Fecha = new Date();

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "IdPermiso", referencedColumnName = "IdPermiso")
    private Permiso Permiso;

    public int getIdGrupoPermiso() {
        return IdGrupoPermiso;
    }

    public void setIdGrupoPermiso(int idGrupoPermiso) {
        IdGrupoPermiso = idGrupoPermiso;
    }

    public Date getFecha() {
        return Fecha;
    }

    public void setFecha(Date fecha) {
        Fecha = fecha;
    }

    public com.bytecode.tratcms.model.entity.Permiso getPermiso() {
        return Permiso;
    }

    public void setPermiso(com.bytecode.tratcms.model.entity.Permiso permiso) {
        Permiso = permiso;
    }
}
