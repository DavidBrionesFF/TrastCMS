package com.bytecode.tratcms.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Date;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Contenido {
	private long IdContenido;
	
	private String Tipo;
	
	private String Contenido;
	
	private long IdPost;

	private Date Fecha;

	public Date getFecha() {
		return Fecha;
	}

	public void setFecha(Date fecha) {
		Fecha = fecha;
	}

	public long getIdContenido() {
		return IdContenido;
	}

	public void setIdContenido(long idContenido) {
		IdContenido = idContenido;
	}

	public String getTipo() {
		return Tipo;
	}

	public void setTipo(String tipo) {
		Tipo = tipo;
	}

	public String getContenido() {
		return Contenido;
	}

	public void setContenido(String contenido) {
		Contenido = contenido;
	}

	public long getIdPost() {
		return IdPost;
	}

	public void setIdPost(long idPost) {
		IdPost = idPost;
	}
}
