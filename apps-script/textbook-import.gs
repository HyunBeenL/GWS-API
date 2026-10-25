/**
 * 서책 목록 시트를 sheet-import-api 서버로 보내 tb_textbook에 저장한다.
 *
 * 설치
 *   1. 서책 스프레드시트에서 확장 프로그램 → Apps Script
 *   2. Code.gs 내용을 지우고 이 파일 내용을 붙여넣은 뒤 저장
 *   3. 스프레드시트를 새로고침하면 상단에 "서책" 메뉴가 생긴다
 *   4. 서책 → 서버에 저장 (처음 한 번은 권한 승인 창이 뜬다)
 *
 * 시트 양식
 *   1행이 헤더(서책코드, 서책명 필수), 2행부터 데이터. A1부터 시작해야 실패 행 번호가 시트와 맞는다.
 *   지금 보고 있는 시트(탭)를 화면에 보이는 값 그대로 보낸다.
 *
 * cloudflared quick tunnel 주소는 cloudflared를 다시 켤 때마다 바뀐다.
 * 그때는 서책 → 서버 주소 설정에서 새 주소를 넣는다.
 */

/** 서버 주소를 설정하지 않았을 때 쓰는 기본값 */
const DEFAULT_API_BASE_URL = 'https://maple-salvation-cordless-peripheral.trycloudflare.com';
const API_BASE_URL_KEY = 'API_BASE_URL';
const IMPORT_PATH = '/api/textbooks/imports/values';
/** 결과 창에 보여 줄 실패 행 수. 전체 목록은 서버의 가져오기 이력(GET /api/import-jobs/{id})에서 본다. */
const MAX_ERRORS_SHOWN = 20;

const STATUS_LABELS = {
  SUCCESS: '성공',
  PARTIAL_SUCCESS: '일부 성공',
  FAILED: '실패',
};

/** 서버가 아니라 cloudflared가 돌려준 응답이거나, 원인을 짐작할 수 있는 상태 코드 */
const HTTP_HINTS = {
  404: '저장 API를 찾을 수 없습니다. 서버를 최신 코드로 다시 시작했는지 확인하세요.',
  502: 'cloudflared는 켜져 있지만 서버(localhost:8080)에 연결하지 못했습니다. 서버가 켜져 있는지 확인하세요.',
  530: 'cloudflared 터널에 연결할 수 없습니다. cloudflared를 다시 실행하고 바뀐 주소를 설정하세요.',
};

function onOpen() {
  SpreadsheetApp.getUi()
    .createMenu('서책')
    .addItem('서버에 저장', 'saveTextbooks')
    .addSeparator()
    .addItem('서버 주소 설정', 'setApiBaseUrl')
    .addToUi();
}

/** 지금 보고 있는 시트를 서버로 보내 저장한다. */
function saveTextbooks() {
  const ui = SpreadsheetApp.getUi();
  const spreadsheet = SpreadsheetApp.getActiveSpreadsheet();
  const sheet = spreadsheet.getActiveSheet();

  // getValues()는 숫자·날짜를 Number·Date로 바꾸므로, 화면에 보이는 문자열을 그대로 읽는다.
  const values = sheet.getDataRange().getDisplayValues();
  if (values.length < 2) {
    ui.alert(`"${sheet.getName()}" 시트에 헤더 아래 데이터가 없습니다.`);
    return;
  }

  const answer = ui.alert(
    '서책 저장',
    `"${sheet.getName()}" 시트의 ${values.length - 1}행을 서버에 저장할까요?\n` +
      '서책코드가 같은 서책이 이미 있으면 시트 값으로 덮어씁니다.',
    ui.ButtonSet.OK_CANCEL);
  if (answer !== ui.Button.OK) {
    return;
  }

  const baseUrl = getApiBaseUrl_();
  let response;
  try {
    response = UrlFetchApp.fetch(baseUrl + IMPORT_PATH, {
      method: 'post',
      contentType: 'application/json',
      payload: JSON.stringify({
        spreadsheetId: spreadsheet.getId(),
        sheetName: sheet.getName(),
        values: values,
      }),
      // 4xx·5xx도 예외 대신 응답으로 받아서 서버가 보낸 오류 메시지를 보여 준다.
      muteHttpExceptions: true,
    });
  } catch (e) {
    // 주소를 찾지 못했거나 시간이 초과되는 등 서버까지 가지 못한 경우
    ui.alert(
      '서버에 연결하지 못했습니다',
      `${baseUrl}\n\ncloudflared와 서버가 켜져 있는지, 주소가 바뀌지 않았는지 확인하세요.\n\n${e.message}`,
      ui.ButtonSet.OK);
    return;
  }

  const status = response.getResponseCode();
  const text = response.getContentText();
  const body = parseJson_(text);
  if (status === 201 && body) {
    ui.alert('서책 저장 결과', formatResult_(body), ui.ButtonSet.OK);
  } else {
    ui.alert(`서책 저장 실패 (HTTP ${status})`, formatProblem_(status, body, text), ui.ButtonSet.OK);
  }
}

/** cloudflared를 다시 켜서 주소가 바뀌었을 때 새 주소를 저장한다. */
function setApiBaseUrl() {
  const ui = SpreadsheetApp.getUi();
  const result = ui.prompt(
    '서버 주소 설정',
    `cloudflared가 알려 준 https://….trycloudflare.com 주소를 넣으세요.\n지금 주소: ${getApiBaseUrl_()}`,
    ui.ButtonSet.OK_CANCEL);
  if (result.getSelectedButton() !== ui.Button.OK) {
    return;
  }

  const url = result.getResponseText().trim().replace(/\/+$/, '');
  if (!/^https:\/\/[^\s/]+$/.test(url)) {
    ui.alert('https:// 로 시작하는 주소만 넣어 주세요. (뒤에 경로는 붙이지 않습니다)');
    return;
  }
  PropertiesService.getScriptProperties().setProperty(API_BASE_URL_KEY, url);
  ui.alert(`서버 주소를 저장했습니다.\n${url}`);
}

function getApiBaseUrl_() {
  return PropertiesService.getScriptProperties().getProperty(API_BASE_URL_KEY) || DEFAULT_API_BASE_URL;
}

/** 응답 예: {"id":3,"status":"PARTIAL_SUCCESS","totalRows":10,"createdCount":7,...,"errors":[{"rowNumber":5,"message":"..."}]} */
function formatResult_(job) {
  const lines = [
    `결과: ${STATUS_LABELS[job.status] || job.status}`,
    `전체 ${job.totalRows}행 → 생성 ${job.createdCount}, 수정 ${job.updatedCount}, ` +
      `변경 없음 ${job.unchangedCount}, 실패 ${job.failedCount}`,
  ];
  const errors = job.errors || [];
  if (errors.length > 0) {
    lines.push('', '저장하지 못한 행:');
    errors.slice(0, MAX_ERRORS_SHOWN).forEach(error => lines.push(`  ${error.rowNumber}행 - ${error.message}`));
    if (errors.length > MAX_ERRORS_SHOWN) {
      lines.push(`  … 외 ${errors.length - MAX_ERRORS_SHOWN}건`);
    }
  }
  lines.push('', `가져오기 이력 #${job.id}`);
  return lines.join('\n');
}

/** 서버 오류는 ProblemDetail({"detail": "...", "code": "..."}) 형식이다. JSON이 아니면 cloudflared 등이 보낸 응답이다. */
function formatProblem_(status, problem, text) {
  const lines = [];
  if (problem && problem.detail) {
    lines.push(problem.code ? `${problem.detail}\n(${problem.code})` : problem.detail);
  } else if (text) {
    lines.push(text.slice(0, 300));
  }
  if (HTTP_HINTS[status]) {
    lines.push(HTTP_HINTS[status]);
  }
  return lines.join('\n\n');
}

function parseJson_(text) {
  try {
    return JSON.parse(text);
  } catch (e) {
    return null;
  }
}
