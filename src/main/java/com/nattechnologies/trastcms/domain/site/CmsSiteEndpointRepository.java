package com.nattechnologies.trastcms.domain.site;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CmsSiteEndpointRepository extends JpaRepository<CmsSiteEndpoint, String> {
    @EntityGraph(attributePaths = "site")
    Optional<CmsSiteEndpoint> findFirstByHostIgnoreCaseAndPortAndActiveTrue(String host, Integer port);
    @EntityGraph(attributePaths = "site")
    Optional<CmsSiteEndpoint> findFirstByHostIgnoreCaseAndPort(String host, Integer port);
    @EntityGraph(attributePaths = "site")
    List<CmsSiteEndpoint> findAllByHostIgnoreCaseAndActiveTrueOrderByPrimaryEndpointDesc(String host);
    @EntityGraph(attributePaths = "site")
    Optional<CmsSiteEndpoint> findFirstBySiteIdAndPrimaryEndpointTrueAndActiveTrue(String siteId);
    List<CmsSiteEndpoint> findAllBySiteIdOrderByPrimaryEndpointDescHostAsc(String siteId);
    boolean existsByHostIgnoreCaseAndPortAndIdNot(String host, Integer port, String id);
    boolean existsByHostIgnoreCaseAndPort(String host, Integer port);
    void deleteAllBySiteId(String siteId);
}
