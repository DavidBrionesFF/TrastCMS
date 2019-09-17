package com.bytecode.tratcms.model;

import java.util.Date;

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
