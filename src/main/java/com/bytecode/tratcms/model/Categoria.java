package com.bytecode.tratcms.model;

import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

public class Categoria {
	private long IdCategoria;

	private String Nombre;
	
	private String Descripcion;

	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
	private Date Fecha;
	
	private long CategoriaSuperior;

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

	public long getCategoriaSuperior() {
		return CategoriaSuperior;
	}

	public void setCategoriaSuperior(long categoriaSuperior) {
		CategoriaSuperior = categoriaSuperior;
	}
}
