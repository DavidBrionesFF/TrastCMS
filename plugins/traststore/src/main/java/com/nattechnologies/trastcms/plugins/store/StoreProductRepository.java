package com.nattechnologies.trastcms.plugins.store;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

interface StoreProductRepository extends JpaRepository<StoreProduct, String> {
    Optional<StoreProduct> findBySlug(String slug);
    Optional<StoreProduct> findBySku(String sku);
    List<StoreProduct> findByDemoDataTrue();
    long countByDemoDataTrue();
    List<StoreProduct> findTop12ByStatusAndFeaturedTrueOrderByUpdatedAtDesc(ProductStatus status);

    @Query("""
            select p from StoreProduct p
            where p.status = :status
              and (:search = ''
                or lower(p.name) like lower(concat('%', :search, '%'))
                or lower(p.sku) like lower(concat('%', :search, '%'))
                or lower(coalesce(p.category, '')) like lower(concat('%', :search, '%')))
            order by p.featured desc, p.updatedAt desc
            """)
    Page<StoreProduct> search(
            @Param("status") ProductStatus status,
            @Param("search") String search,
            Pageable pageable);
}
