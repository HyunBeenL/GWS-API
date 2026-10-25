package com.leehv1234.sheetimport.global.error;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * 모든 오류를 RFC 9457 ProblemDetail 형식(application/problem+json)으로 응답한다.
 *
 * <p>요청 형식 오류(검증 실패, 파일 누락, 업로드 용량 초과 등)는 부모 클래스
 * {@link ResponseEntityExceptionHandler}가 처리하고, 여기서는 애플리케이션 예외만 다룬다.
 * 응답의 {@code code} 필드로 클라이언트가 오류 종류를 구분할 수 있다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    ProblemDetail handleBusiness(BusinessException e) {
        ErrorCode code = e.getErrorCode();
        if (code.getStatus().is5xxServerError()) {
            log.error("[{}] {}", code, e.getMessage(), e);
        } else {
            log.warn("[{}] {}", code, e.getMessage());
        }
        return problem(code, e.getMessage());
    }

    /** 두 요청이 같은 키로 동시에 새 행을 넣으려 할 때 unique 제약에 걸린다. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleDataConflict(DataIntegrityViolationException e) {
        log.warn("데이터 무결성 위반", e);
        return problem(ErrorCode.DATA_CONFLICT, ErrorCode.DATA_CONFLICT.getMessage());
    }

    /** {@code ?sort=없는필드} 처럼 정렬 기준이 엔티티에 없을 때 */
    @ExceptionHandler(PropertyReferenceException.class)
    ProblemDetail handleBadSort(PropertyReferenceException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "정렬할 수 없는 필드입니다: " + e.getPropertyName());
        problem.setProperty("code", "INVALID_SORT");
        return problem;
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception e) {
        log.error("처리하지 못한 예외", e);
        return problem(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.getMessage());
    }

    private static ProblemDetail problem(ErrorCode code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(code.getStatus(), detail);
        problem.setProperty("code", code.name());
        return problem;
    }
}
