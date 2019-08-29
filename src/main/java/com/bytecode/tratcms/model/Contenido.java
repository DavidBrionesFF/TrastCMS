package com.bytecode.tratcms.model;

public class Contenido {
	private long IdContenido;
	
	private String Tipo;
	
	private String Contenido;
	
	private long IdPost;

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
