/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bytecode.tratcms.model.entity;

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
@Table(name = "comentario_metadata", catalog = "blog", schema = "")
@NamedQueries({
    @NamedQuery(name = "ComentarioMetadata.findAll", query = "SELECT c FROM ComentarioMetadata c")})
public class ComentarioMetadata implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "IdComentarioMetadta")
    private Integer idComentarioMetadta;
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
    @JoinColumn(name = "IdComentario", referencedColumnName = "IdComentario")
    @ManyToOne(optional = false)
    private Comentario idComentario;

    public ComentarioMetadata() {
    }

    public ComentarioMetadata(Integer idComentarioMetadta) {
        this.idComentarioMetadta = idComentarioMetadta;
    }

    public ComentarioMetadata(Integer idComentarioMetadta, String clave, String valor, String tipo) {
        this.idComentarioMetadta = idComentarioMetadta;
        this.clave = clave;
        this.valor = valor;
        this.tipo = tipo;
    }

    public Integer getIdComentarioMetadta() {
        return idComentarioMetadta;
    }

    public void setIdComentarioMetadta(Integer idComentarioMetadta) {
        this.idComentarioMetadta = idComentarioMetadta;
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

    public Comentario getIdComentario() {
        return idComentario;
    }

    public void setIdComentario(Comentario idComentario) {
        this.idComentario = idComentario;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (idComentarioMetadta != null ? idComentarioMetadta.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof ComentarioMetadata)) {
            return false;
        }
        ComentarioMetadata other = (ComentarioMetadata) object;
        if ((this.idComentarioMetadta == null && other.idComentarioMetadta != null) || (this.idComentarioMetadta != null && !this.idComentarioMetadta.equals(other.idComentarioMetadta))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.bytecode.tratcms.model.entity.ComentarioMetadata[ idComentarioMetadta=" + idComentarioMetadta + " ]";
    }
    
}
