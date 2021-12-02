/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bytecode.tratcms.data.model.entity;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;

/**
 *
 * @author David
 */
@Entity
@Table(name = "grupo_permiso", catalog = "blog", schema = "")
@NamedQueries({
    @NamedQuery(name = "GrupoPermiso.findAll", query = "SELECT g FROM GrupoPermiso g")})
public class GrupoPermiso implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "IdGrupoPermiso")
    private Integer idGrupoPermiso;
    @Column(name = "Fecha")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fecha;
    @Basic(optional = false)
    @NotNull
    @Column(name = "IdPermiso")
    private long idPermiso;
    @Basic(optional = false)
    @NotNull
    @Column(name = "IdGrupo")
    private long idGrupo;

    public GrupoPermiso() {
    }

    public GrupoPermiso(Integer idGrupoPermiso) {
        this.idGrupoPermiso = idGrupoPermiso;
    }

    public GrupoPermiso(Integer idGrupoPermiso, long idPermiso, long idGrupo) {
        this.idGrupoPermiso = idGrupoPermiso;
        this.idPermiso = idPermiso;
        this.idGrupo = idGrupo;
    }

    public Integer getIdGrupoPermiso() {
        return idGrupoPermiso;
    }

    public void setIdGrupoPermiso(Integer idGrupoPermiso) {
        this.idGrupoPermiso = idGrupoPermiso;
    }

    public Date getFecha() {
        return fecha;
    }

    public void setFecha(Date fecha) {
        this.fecha = fecha;
    }

    public long getIdPermiso() {
        return idPermiso;
    }

    public void setIdPermiso(long idPermiso) {
        this.idPermiso = idPermiso;
    }

    public long getIdGrupo() {
        return idGrupo;
    }

    public void setIdGrupo(long idGrupo) {
        this.idGrupo = idGrupo;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (idGrupoPermiso != null ? idGrupoPermiso.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof GrupoPermiso)) {
            return false;
        }
        GrupoPermiso other = (GrupoPermiso) object;
        if ((this.idGrupoPermiso == null && other.idGrupoPermiso != null) || (this.idGrupoPermiso != null && !this.idGrupoPermiso.equals(other.idGrupoPermiso))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.bytecode.tratcms.data.model.entity.GrupoPermiso[ idGrupoPermiso=" + idGrupoPermiso + " ]";
    }
    
}
