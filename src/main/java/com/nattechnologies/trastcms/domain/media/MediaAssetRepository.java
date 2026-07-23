package com.nattechnologies.trastcms.domain.media;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MediaAssetRepository extends JpaRepository<MediaAsset, String> {
    List<MediaAsset> findTop500ByOrderByCreatedAtDesc();
}
