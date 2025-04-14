# MCP 애플리케이션 API 명세서

이 문서는 MCP 애플리케이션에서 제공하는 API 엔드포인트를 설명합니다.

## 채팅 API

### 질문 처리

**요청**
- URL: `/api/chat`
- 메서드: `POST`
- 컨텐츠 타입: `application/json`

**요청 본문**
```json
{
  "query": "여기에 질문 내용을 입력합니다",
  "sessionId": "세션 ID (선택사항)"
}
```

**응답**
- 상태 코드: `200 OK`
- 컨텐츠 타입: `application/json`

```json
{
  "response": "AI 응답 내용",
  "sessionId": "세션 ID",
  "responseType": "text | visualization | structured | error",
  "metadata": "관련 메타데이터 정보"
}
```

### 상태 확인

**요청**
- URL: `/api/chat/health`
- 메서드: `GET`

**응답**
- 상태 코드: `200 OK`
- 컨텐츠 타입: `text/plain`
- 본문: `Service is up and running`

## 문서 API

### 문서 업로드

**요청**
- URL: `/api/documents/upload`
- 메서드: `POST`
- 컨텐츠 타입: `multipart/form-data`

**폼 필드**
- `file`: 업로드할 파일
- `source`: 파일 출처 (선택사항, 기본값: "user_upload")

**응답**
- 상태 코드: `200 OK`
- 컨텐츠 타입: `text/plain`
- 본문: `Document uploaded and processed successfully` 또는 오류 메시지

### 문서 목록 조회

**요청**
- URL: `/api/documents`
- 메서드: `GET`

**응답**
- 상태 코드: `200 OK`
- 컨텐츠 타입: `application/json`
- 본문: 문서 목록 배열

```json
[
  {
    "id": 1,
    "documentId": "문서 고유 ID",
    "title": "문서 제목",
    "source": "문서 출처",
    "content": "문서 내용 일부",
    "createdAt": "2023-04-09T12:00:00.000Z"
  },
  ...
]
```

### 문서 삭제

**요청**
- URL: `/api/documents/{documentId}`
- 메서드: `DELETE`
- 경로 변수:
  - `documentId`: 삭제할 문서의 고유 ID

**응답**
- 상태 코드: `200 OK`
- 컨텐츠 타입: `text/plain`
- 본문: `Document deleted successfully` 또는 오류 메시지

## MCP 도구

이 애플리케이션은 다음 MCP 도구를 내부적으로 사용합니다:

### 파일시스템 MCP

파일 시스템 접근을 위한 도구:
- 파일 읽기
- 파일 쓰기
- 디렉토리 생성 및 관리
- 파일 목록 조회

### Brave 검색 MCP

웹 검색 기능을 위한 도구:
- 웹 검색 쿼리 실행
- 검색 결과 필터링 및 포맷팅

### PostgreSQL MCP

데이터베이스 접근을 위한 도구:
- SQL SELECT 쿼리 실행
- 데이터베이스 스키마 정보 조회

## 오류 응답

모든 API는 오류 상황에서 다음과 같은 응답을 반환할 수 있습니다:

- 상태 코드: `400 Bad Request` - 잘못된 요청
- 상태 코드: `404 Not Found` - 리소스를 찾을 수 없음
- 상태 코드: `500 Internal Server Error` - 서버 오류

오류 응답의 본문은 오류 메시지를 포함한 텍스트 형식이거나 다음 JSON 형식일 수 있습니다:

```json
{
  "error": "오류 유형",
  "message": "오류 메시지",
  "timestamp": "2023-04-09T12:00:00.000Z"
}
```

## 인증

현재 버전에서는 인증 메커니즘이 구현되어 있지 않습니다. 프로덕션 환경에서는 적절한 인증 및 권한 부여 메커니즘을 구현하는 것이 권장됩니다.
