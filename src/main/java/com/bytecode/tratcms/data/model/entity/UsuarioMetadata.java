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
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 *
 * @author David
 */
@Entity
@Table(name = "usuario_metadata", catalog = "blog", schema = "")
@NamedQueries({
    @NamedQuery(name = "UsuarioMetadata.findAll", query = "SELECT u FROM UsuarioMetadata u")})
public class UsuarioMetadata implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "IdUsuarioMetadata")
    private Integer idUsuarioMetadata;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "Clave")
    private String clave;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "Valor")
    private String valor;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "Tipo")
    private String tipo;
    @Column(name = "Fecha")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fecha;
    @JoinColumn(name = "IdUsuario", referencedColumnName = "IdUsuario")
    @ManyToOne(optional = false)
    private Usuario idUsuario;

    public UsuarioMetadata() {
    }

    public UsuarioMetadata(Integer idUsuarioMetadata) {
        this.idUsuarioMetadata = idUsuarioMetadata;
    }

    public UsuarioMetadata(Integer idUsuarioMetadata, String clave, String valor, String tipo) {
        this.idUsuarioMetadata = idUsuarioMetadata;
        this.clave = clave;
        this.valor = valor;
        this.tipo = tipo;
    }

    public Integer getIdUsuarioMetadata() {
        return idUsuarioMetadata;
    }

    public void setIdUsuarioMetadata(Integer idUsuarioMetadata) {
        this.idUsuarioMetadata = idUsuarioMetadata;
    }

    public String getClave() {
        return clave;
    }

    public void setClave(String clave) {
        this.clave = clave;
    }

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public Date getFecha() {
        return fecha;
    }

    public void setFecha(Date fecha) {
        this.fecha = fecha;
    }

    public Usuario getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Usuario idUsuario) {
        this.idUsuario = idUsuario;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (idUsuarioMetadata != null ? idUsuarioMetadata.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof UsuarioMetadata)) {
            return false;
        }
        UsuarioMetadata other = (UsuarioMetadata) object;
        if ((this.idUsuarioMetadata == null && other.idUsuarioMetadata != null) || (this.idUsuarioMetadata != null && !this.idUsuarioMetadata.equals(other.idUsuarioMetadata))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.bytecode.tratcms.data.model.entity.UsuarioMetadata[ idUsuarioMetadata=" + idUsuarioMetadata + " ]";
    }
    
}
