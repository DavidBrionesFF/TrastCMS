package com.nattechnologies.trastcms.plugins.academy;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
interface AcademyImportJobRepository extends JpaRepository<AcademyImportJob,String> {
    List<AcademyImportJob> findTop20ByOrderByStartedAtDesc();
}
