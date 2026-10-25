package com.leehv1234.sheetimport.spreadsheet;

/**
 * 가져올 데이터가 어디에 있는지를 나타낸다.
 *
 * <p>sealed로 구현체를 제한해 두었기 때문에, 소스를 새로 추가하면 {@code switch (source)}로
 * 분기하는 곳에서 컴파일 에러가 나서 처리 누락을 바로 알 수 있다.
 */
public sealed interface SheetSource permits ExcelFileSource, GoogleSheetSource {

    SheetSourceType type();

    /** 가져오기 이력에 남길 출처 (파일명, 스프레드시트 ID 등) */
    String description();
}
