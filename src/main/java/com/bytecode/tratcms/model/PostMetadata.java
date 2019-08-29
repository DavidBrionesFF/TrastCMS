package com.bytecode.tratcms.model;

public class PostMetadata {
	private long IdPostMetadata;
	
	private long IdPost;
	
	private String Clave;
	
	private String Valor;
	
	private String Tipo;

	public long getIdPostMetadata() {
		return IdPostMetadata;
	}

	public void setIdPostMetadata(long idPostMetadata) {
		IdPostMetadata = idPostMetadata;
	}

	public long getIdPost() {
		return IdPost;
	}

	public void setIdPost(long idPost) {
		IdPost = idPost;
	}

	public String getClave() {
		return Clave;
	}

	public void setClave(String clave) {
		Clave = clave;
	}

	public String getValor() {
		return Valor;
	}

	public void setValor(String valor) {
		Valor = valor;
	}

	public String getTipo() {
		return Tipo;
	}

	public void setTipo(String tipo) {
		Tipo = tipo;
	}
}
