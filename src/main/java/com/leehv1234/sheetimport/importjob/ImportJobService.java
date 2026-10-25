package com.leehv1234.sheetimport.importjob;

import com.leehv1234.sheetimport.global.error.BusinessException;
import com.leehv1234.sheetimport.global.error.ErrorCode;
import com.leehv1234.sheetimport.global.web.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ImportJobService {

    private final ImportJobRepository importJobRepository;

    public ImportJobResponse get(Long id) {
        return importJobRepository.findWithErrorsById(id)
                .map(ImportJobResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.IMPORT_JOB_NOT_FOUND));
    }

    public PageResponse<ImportJobSummaryResponse> list(Pageable pageable) {
        return PageResponse.from(importJobRepository.findAll(pageable).map(ImportJobSummaryResponse::from));
    }
}
