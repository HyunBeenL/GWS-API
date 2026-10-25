package com.leehv1234.sheetimport.textbook;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface TextbookRepository extends JpaRepository<Textbook, Long> {

    /** 시트에 있는 서책코드들을 IN 쿼리 한 번으로 조회한다. 삭제된 서책(deleted_at 있음)은 뺀다. */
    List<Textbook> findAllByCodeInAndDeletedAtIsNull(Collection<String> codes);
}
