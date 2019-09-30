package com.bytecode.tratcms.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Date;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Grupo {
	private long Idgrupo;
	
	private String Nombre;

	private Date Fecha;

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
}
