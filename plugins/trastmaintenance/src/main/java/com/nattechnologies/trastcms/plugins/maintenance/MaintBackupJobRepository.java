package com.nattechnologies.trastcms.plugins.maintenance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MaintBackupJobRepository extends JpaRepository<MaintBackupJob, String> {
}
