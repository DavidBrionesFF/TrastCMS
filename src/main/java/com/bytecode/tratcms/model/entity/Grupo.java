package com.bytecode.tratcms.model.entity;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "grupo")
public class Grupo implements Serializable {
    @Id
    @Column(name = "IdGrupo")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long Idgrupo;

    @Column(name = "Nombre")
    private String Nombre;

    @Column(name = "Fecha")
    private Date Fecha;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "grupo")
    private List<GrupoPermiso> grupoPermisos;

    public Date getFecha() {
        return Fecha;
    }

    public void setFecha(Date fecha) {
        Fecha = fecha;
    }

    public long getIdgrupo() {
        return Idgrupo;
    }

    public void setIdgrupo(long idgrupo) {
        Idgrupo = idgrupo;
    }

    public String getNombre() {
        return Nombre;
    }

    public void setNombre(String nombre) {
        Nombre = nombre;
    }

    public List<GrupoPermiso> getGrupoPermisos() {
        return grupoPermisos;
    }

    public void setGrupoPermisos(List<GrupoPermiso> grupoPermisos) {
        this.grupoPermisos = grupoPermisos;
    }

    //Metodos extra...
    public List<Permiso> getPermisos(){
        return getGrupoPermisos()
                .stream()
                .map(grupoPermiso -> grupoPermiso.getPermiso())
                .collect(Collectors.toList());
    }
}
