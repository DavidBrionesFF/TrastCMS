package com.bytecode.tratcms.model.entity;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "usuario")
public class Usuario implements Serializable {
    @Id
    @Column(name = "IdUsuario")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long IdUsuario;

    @Column(name = "Nombre")
    private String Nombre;

    @Column(name = "Apellido")
    private String Apellido;

    @Column(name = "Contrasena")
    private String Contrasena;

    @Column(name = "Correo")
    private String Correo;

    @Column(name = "Fecha")
    @Temporal(TemporalType.DATE)
    private Date Fecha;

    @ManyToMany(mappedBy = "usuarios", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Post> postsGuardados;

    public long getIdUsuario() {
        return IdUsuario;
    }

    public void setIdUsuario(long idUsuario) {
        IdUsuario = idUsuario;
    }

    public String getNombre() {
        return Nombre;
    }

    public void setNombre(String nombre) {
        Nombre = nombre;
    }

    public String getApellido() {
        return Apellido;
    }

    public void setApellido(String apellido) {
        Apellido = apellido;
    }

    public String getContrasena() {
        return Contrasena;
    }

    public void setContrasena(String contrasena) {
        Contrasena = contrasena;
    }

    public String getCorreo() {
        return Correo;
    }

    public void setCorreo(String correo) {
        Correo = correo;
    }

    public Date getFecha() {
        return Fecha;
    }

    public void setFecha(Date fecha) {
        Fecha = fecha;
    }

    public List<Post> getPostsGuardados() {
        return postsGuardados;
    }

    public void setPostsGuardados(List<Post> postsGuardados) {
        this.postsGuardados = postsGuardados;
    }
}
