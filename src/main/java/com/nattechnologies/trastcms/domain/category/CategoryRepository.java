package com.nattechnologies.trastcms.domain.category;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, String> {
    Optional<Category> findBySlug(String slug);
    boolean existsBySlug(String slug);
    boolean existsByNameIgnoreCase(String name);
    boolean existsBySlugAndIdNot(String slug, String id);
    boolean existsByNameIgnoreCaseAndIdNot(String name, String id);
    List<Category> findAllByOrderByNameAsc();
}
