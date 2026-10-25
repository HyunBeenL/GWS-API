package com.leehv1234.sheetimport.spreadsheet;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GoogleSheetSourceTest {

    @Test
    void URL을_넣어도_스프레드시트_ID만_뽑아낸다() {
        GoogleSheetSource source = new GoogleSheetSource(
                "https://docs.google.com/spreadsheets/d/1AbC-xyz_09/edit#gid=0", "상품목록!A1:E");

        assertThat(source.spreadsheetId()).isEqualTo("1AbC-xyz_09");
        assertThat(source.range()).isEqualTo("상품목록!A1:E");
    }

    @Test
    void 범위를_비우면_첫_번째_시트의_A부터_Z열() {
        GoogleSheetSource source = new GoogleSheetSource(" 1AbC-xyz_09 ", " ");

        assertThat(source.spreadsheetId()).isEqualTo("1AbC-xyz_09");
        assertThat(source.range()).isEqualTo(GoogleSheetSource.DEFAULT_RANGE);
    }
}
