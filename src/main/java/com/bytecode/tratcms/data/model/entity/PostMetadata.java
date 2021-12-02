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
@Table(name = "post_metadata", catalog = "blog", schema = "")
@NamedQueries({
    @NamedQuery(name = "PostMetadata.findAll", query = "SELECT p FROM PostMetadata p")})
public class PostMetadata implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "IdPostMetadata")
    private Integer idPostMetadata;
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
    @JoinColumn(name = "IdPost", referencedColumnName = "IdPost")
    @ManyToOne(optional = false)
    private Post idPost;

    public PostMetadata() {
    }

    public PostMetadata(Integer idPostMetadata) {
        this.idPostMetadata = idPostMetadata;
    }

    public PostMetadata(Integer idPostMetadata, String clave, String valor, String tipo) {
        this.idPostMetadata = idPostMetadata;
        this.clave = clave;
        this.valor = valor;
        this.tipo = tipo;
    }

    public Integer getIdPostMetadata() {
        return idPostMetadata;
    }

    public void setIdPostMetadata(Integer idPostMetadata) {
        this.idPostMetadata = idPostMetadata;
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

    public Post getIdPost() {
        return idPost;
    }

    public void setIdPost(Post idPost) {
        this.idPost = idPost;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (idPostMetadata != null ? idPostMetadata.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof PostMetadata)) {
            return false;
        }
        PostMetadata other = (PostMetadata) object;
        if ((this.idPostMetadata == null && other.idPostMetadata != null) || (this.idPostMetadata != null && !this.idPostMetadata.equals(other.idPostMetadata))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.bytecode.tratcms.data.model.entity.PostMetadata[ idPostMetadata=" + idPostMetadata + " ]";
    }
    
}
