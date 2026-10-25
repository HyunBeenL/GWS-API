package com.leehv1234.sheetimport.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 엑셀 업로드
    EMPTY_FILE(HttpStatus.BAD_REQUEST, "업로드된 파일이 비어 있습니다."),
    UNSUPPORTED_FILE_TYPE(HttpStatus.BAD_REQUEST, "엑셀 파일(.xlsx, .xls)만 업로드할 수 있습니다."),
    INVALID_FILE(HttpStatus.BAD_REQUEST, "엑셀 파일을 읽을 수 없습니다. 손상되었거나 암호가 걸린 파일인지 확인하세요."),
    SHEET_NOT_FOUND(HttpStatus.BAD_REQUEST, "시트를 찾을 수 없습니다."),

    // 시트 양식
    EMPTY_SHEET(HttpStatus.BAD_REQUEST, "시트가 비어 있습니다. 첫 행에 헤더가 있어야 합니다."),
    MISSING_COLUMNS(HttpStatus.BAD_REQUEST, "필수 컬럼이 없습니다."),
    NO_DATA_ROWS(HttpStatus.BAD_REQUEST, "헤더 아래에 데이터 행이 없습니다."),

    // Google Sheets
    GOOGLE_SHEETS_NOT_CONFIGURED(HttpStatus.SERVICE_UNAVAILABLE,
            "Google Sheets 연동이 설정되지 않았습니다. google.sheets.credentials 를 지정하세요."),
    GOOGLE_SHEETS_BAD_REQUEST(HttpStatus.BAD_REQUEST, "Google Sheets 요청이 잘못되었습니다. 범위(range) 표기를 확인하세요."),
    GOOGLE_SHEETS_ACCESS_DENIED(HttpStatus.FORBIDDEN,
            "스프레드시트에 접근할 수 없습니다. 서비스 계정 이메일에 시트를 공유했는지, Sheets API를 사용 설정했는지 확인하세요."),
    GOOGLE_SHEETS_NOT_FOUND(HttpStatus.NOT_FOUND, "스프레드시트를 찾을 수 없습니다. ID를 확인하세요."),
    GOOGLE_SHEETS_API_ERROR(HttpStatus.BAD_GATEWAY, "Google Sheets API 호출에 실패했습니다."),

    // 공통
    IMPORT_JOB_NOT_FOUND(HttpStatus.NOT_FOUND, "가져오기 이력을 찾을 수 없습니다."),
    DATA_CONFLICT(HttpStatus.CONFLICT, "다른 요청과 데이터가 충돌했습니다. 잠시 후 다시 시도하세요."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;
}
