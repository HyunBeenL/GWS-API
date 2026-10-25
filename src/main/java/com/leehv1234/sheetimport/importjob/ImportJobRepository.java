package com.leehv1234.sheetimport.importjob;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ImportJobRepository extends JpaRepository<ImportJob, Long> {

    /** 실패 행 목록까지 한 번의 조회로 가져온다. */
    @EntityGraph(attributePaths = "errors")
    Optional<ImportJob> findWithErrorsById(Long id);
}
