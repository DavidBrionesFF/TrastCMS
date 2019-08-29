package com.bytecode.tratcms.model;

import java.util.Date;

public class Categoria {
	private long IdCategoria;
	
	private String Nombre;
	
	private String Descripcion;
	
	private Date Fecha;
	
	private long CategoriaSuperiorior;

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

	public long getCategoriaSuperiorior() {
		return CategoriaSuperiorior;
	}

	public void setCategoriaSuperiorior(long categoriaSuperiorior) {
		CategoriaSuperiorior = categoriaSuperiorior;
	}
}
