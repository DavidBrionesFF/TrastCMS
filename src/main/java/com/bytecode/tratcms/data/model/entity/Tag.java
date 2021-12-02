/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bytecode.tratcms.data.model.entity;

import java.io.Serializable;
import java.math.BigInteger;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 *
 * @author David
 */
@Entity
@Table(name = "tag", catalog = "blog", schema = "")
@NamedQueries({
    @NamedQuery(name = "Tag.findAll", query = "SELECT t FROM Tag t")})
public class Tag implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id_tag")
    private Integer idTag;
    @Size(max = 255)
    @Column(name = "nombre")
    private String nombre;
    @Column(name = "id_categoria")
    private BigInteger idCategoria;
    @Basic(optional = false)
    @NotNull
    @Column(name = "IdTag")
    private int idTag1;
    @Column(name = "IdCategoria")
    private BigInteger idCategoria1;

    public Tag() {
    }

    public Tag(Integer idTag) {
        this.idTag = idTag;
    }

    public Tag(Integer idTag, int idTag1) {
        this.idTag = idTag;
        this.idTag1 = idTag1;
    }

    public Integer getIdTag() {
        return idTag;
    }

    public void setIdTag(Integer idTag) {
        this.idTag = idTag;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public BigInteger getIdCategoria() {
        return idCategoria;
    }

    public void setIdCategoria(BigInteger idCategoria) {
        this.idCategoria = idCategoria;
    }

    public int getIdTag1() {
        return idTag1;
    }

    public void setIdTag1(int idTag1) {
        this.idTag1 = idTag1;
    }

    public BigInteger getIdCategoria1() {
        return idCategoria1;
    }

    public void setIdCategoria1(BigInteger idCategoria1) {
        this.idCategoria1 = idCategoria1;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (idTag != null ? idTag.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Tag)) {
            return false;
        }
        Tag other = (Tag) object;
        if ((this.idTag == null && other.idTag != null) || (this.idTag != null && !this.idTag.equals(other.idTag))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.bytecode.tratcms.data.model.entity.Tag[ idTag=" + idTag + " ]";
    }
    
}
