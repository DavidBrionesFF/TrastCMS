package com.bytecode.tratcms;

import com.bytecode.tratcms.data.repository.jpa.JpaCategoriaRepository;
import com.bytecode.tratcms.logic.service.InstalacionService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

@SpringBootApplication
@EntityScan("com.bytecode.tratcms.data.model.entity")
@EnableJpaRepositories("com.bytecode.tratcms.data.repository.jpa")
@Transactional
public class Application implements CommandLineRunner {
	private Log logger = LogFactory.getLog(getClass());
	@Autowired
	private InstalacionService instalacionService;

	@Autowired
	private JpaCategoriaRepository jpaCategoriaRepository;

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Autowired
	private EntityManager entityManager;

	@Override
	public void run(String... args) throws Exception {
		instalacionService.init();

		//CRUD EN JPA

		//CREAR
//		Categoria categoria = new Categoria();
//
//		categoria.setDescripcion("Esta es una descripcion de ejemplo");
//		categoria.setFecha(new Date());
//		categoria.setNombre("Categoria de Ejemplo Con Entity Manager");

		//Iniciamos transaccion
		//entityManager.persist(categoria);

//		entityManager.createQuery("select c from Categoria c")
//				.getResultList()
//				.forEach(o -> {
//					Categoria c = (Categoria) o;
//					c.setNombre("I -");
//					entityManager.remove(o);
//					logger.info(o);
//				});

		//Hacer commit
//
//		categoria = jpaCategoriaRepository.save(categoria);

		//ACTUALIZAR
		//Categoria categoria;
		//categoria = jpaCategoriaRepository.findById(1).get();
		//categoria.setNombre("Actualizacion de Ejemplo");

		//jpaCategoriaRepository.save(categoria);

		// OBTENER
//		jpaCategoriaRepository.findAll()
//				.forEach(categoria -> {
//					logger.info("La categoria es: " + categoria.getIdCategoria() );
//				});

		//jpaCategoriaRepository.deleteById(1);

//		categoria = jpaCategoriaRepository.findByNombre("Categoria de Ejemplo").get();
//
//		logger.info("La categoria es: " + categoria.getIdCategoria());
//
//		jpaCategoriaRepository.findByNombreLike("C")
//				.forEach(categoria1 -> {
//					logger.info("La categoria es: " + categoria1.getIdCategoria());
//				});
	}
}
