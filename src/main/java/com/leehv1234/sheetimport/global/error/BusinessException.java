package com.leehv1234.sheetimport.global.error;

import lombok.Getter;

/**
 * 예상할 수 있는 실패(잘못된 파일, 시트 권한 없음 등)를 나타낸다.
 * {@link GlobalExceptionHandler}가 {@link ErrorCode}의 상태 코드로 바꿔 응답한다.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, errorCode.getMessage(), null);
    }

    public BusinessException(ErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }

    public BusinessException(ErrorCode errorCode, Throwable cause) {
        this(errorCode, errorCode.getMessage(), cause);
    }

    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
