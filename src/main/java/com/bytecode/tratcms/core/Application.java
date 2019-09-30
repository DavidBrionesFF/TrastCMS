package com.bytecode.tratcms.core;

import com.bytecode.tratcms.core.repository.UsuarioRep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class Application implements CommandLineRunner {
	@Autowired
	private Environment environment;

	@Autowired
	private UsuarioRep usuarioRep;

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
//		Usuario usuario = new Usuario();
//		usuario.setIdUsuario(1);
//		usuario.setNombre("admin");
//		usuario.setCorreo("admin@bytepl.com");
//		usuario.setContrasena(new BCryptPasswordEncoder().encode("1234"));
//		usuario.setApellido("admin");
//
//		usuarioRep.save(usuario);
	}
}
