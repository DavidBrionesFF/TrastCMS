package com.bytecode.tratcms.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Date;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class GrupoPermiso {
	private long IdGrupo;
	
	private long IdPermiso;

	private Date Fecha;

	public Date getFecha() {
		return Fecha;
	}

	public void setFecha(Date fecha) {
		Fecha = fecha;
	}

	public long getIdGrupo() {
		return IdGrupo;
	}

	public void setIdGrupo(long idGrupo) {
		IdGrupo = idGrupo;
	}

	public long getIdPermiso() {
		return IdPermiso;
	}

	public void setIdPermiso(long idPermiso) {
		IdPermiso = idPermiso;
	}
}
