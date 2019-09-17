create database blog;
use blog;

create table categoria(
	IdCategoria int primary key not null auto_increment,
	Nombre varchar(255) not null,
	Descripcion varchar(255),
	Fecha datetime default current_timestamp,
	CategoriaSuperior long
);

create table grupo(
	IdGrupo int primary key not null auto_increment,
	Nombre varchar(255) not null,
	Fecha datetime default current_timestamp
);

create table Permiso(
	IdPermiso int primary key not null auto_increment,
	Nombre varchar(255) not null,
	Fecha datetime default current_timestamp
);

create table grupo_permiso(
	IdGrupoPermiso int primary key not null auto_increment,
	IdPermiso int not null,
	IdGrupo int not null,
	Fecha datetime default current_timestamp,
	foreign key(IdPermiso) references permiso(IdPermiso),
	foreign key(IdGrupo) references grupo(IdGrupo)
);

create table usuario(
	 IdUsuario int primary key not null auto_increment,
	 Nombre varchar(255) not null,
	 Apellido varchar(255),
	 Fecha datetime default current_timestamp,
	 Contrasena varchar(255) not null,
	 Correo varchar(255) not null unique,
	 IdGrupo int not null
);

create table usuario_metadata (
	IdUsuarioMetadata int primary key not null auto_increment,
	IdUsuario int not null,
    Clave varchar(255) not null,
    Valor varchar(255) not null,
	Tipo varchar(255) not null,
	Fecha datetime default current_timestamp,
	foreign key(IdUsuario) references usuario(IdUsuario)
);

create table post(
	IdPost int primary key not null auto_increment,
	Titulo varchar(255) not null,
	Slug varchar(255),
	Extracto varchar(255),
	IdUsuario int not null,
	IdCategoria int not null,
	ImagenDestacada varchar(255),
	Tipo varchar(255),
	Fecha datetime default current_timestamp,
	foreign key(IdUsuario) references usuario(IdUsuario),
	foreign key(IdCategoria) references categoria(IdCategoria)
);

create table post_metadata(
	IdPostMetadata int primary key not null auto_increment,
	IdPost int not null,
	Clave varchar(255) not null,
    Valor varchar(255) not null,
	Tipo varchar(255) not null,
	Fecha datetime default current_timestamp,
	foreign key(IdPost) references post(IdPost)
);

create table contenido(
	IdContenido int primary key not null auto_increment,
	Tipo varchar(255) not null,
	Contenido varchar(255) not null,
	Fecha datetime default current_timestamp,
	IdPost int not null,
	foreign key(IdPost) references post(IdPost)
);

create table comentario(
	IdComentario int primary key not null auto_increment,
	Comentario varchar(255) not null,
	Respuesta varchar(255),
	IdUsuario int not null,
	IdPost int not null,
	Fecha datetime default current_timestamp,
	foreign key(IdUsuario) references usuario(IdUsuario),
	foreign key(IdPost) references post(IdPost)
);