# MCP 애플리케이션

이 스프링 부트 애플리케이션은 OpenAI API와 MCP(Model Context Protocol)를 통합하여 벡터 데이터베이스를 활용한 지능형 문서 검색 및 질의응답 시스템을 제공합니다.

## 아키텍처

애플리케이션은 다음과 같은 처리 흐름을 따릅니다:

```
[사용자 질문 입력]
       ↓
[질문을 벡터로 변환] ← OpenAI 임베딩 API
       ↓
[유사 문서 검색] ← Qdrant 벡터 DB
       ↓
[문맥 기반 프롬프트 구성] ← resources의 템플릿 활용
       ↓
[MCP 도구와 함께 OpenAI GPT API 호출]
       ↓
[응답 처리] ← 응답 유형에 따른 포맷팅
       ↓
[사용자에게 응답 반환]
```

## MCP 도구 통합

이 애플리케이션은 다음 MCP 도구와 통합되어 있습니다:

1. **파일시스템 MCP 도구**: 파일 읽기 및 쓰기 기능
2. **Brave 검색 MCP 도구**: 웹 검색 기능 
3. **PostgreSQL MCP 도구**: 구조화된 데이터 접근 기능

## 주요 구성 요소

- **벡터 임베딩**: 텍스트를 벡터 표현으로 변환
- **벡터 데이터베이스(Qdrant)**: 문서 벡터 저장 및 검색
- **MCP 클라이언트/서버**: 외부 도구 및 리소스와 인터페이스
- **OpenAI 통합**: 지능형 응답을 위한 GPT 모델 활용

## 설정 및 배포

### 사전 요구 사항

- JDK 17 이상
- Docker 및 Docker Compose
- OpenAI API 키
- Brave 검색 API 키 (선택사항)

### 환경 변수

다음 환경 변수를 설정하세요:
- `OPENAI_API_KEY`: OpenAI API 키
- `BRAVE_SEARCH_API_KEY`: Brave 검색 API 키 (Brave 검색 사용 시)

### Docker Compose로 실행하기

```bash
# 모든 서비스 빌드 및 시작
docker-compose up -d

# 로그 보기
docker-compose logs -f

# 모든 서비스 중지
docker-compose down
```

### API 접근

주요 API는 다음 주소에서 사용할 수 있습니다:
- 채팅 API: `POST http://localhost:8080/api/chat`
- 문서 업로드: `POST http://localhost:8080/api/documents/upload`
- 문서 목록: `GET http://localhost:8080/api/documents`

## 개발

### 프로젝트 구조

- `config/`: 애플리케이션 설정
- `controller/`: REST API 엔드포인트
- `model/`: 도메인 모델
- `repository/`: 데이터 접근 인터페이스
- `service/`: 비즈니스 로직
- `mcp/`: MCP 도구 설정
- `util/`: 유틸리티 클래스
- `resources/`: 템플릿 및 설정 파일

### 프로젝트 빌드

```bash
./gradlew clean build
```

## 커스터마이징

- `resources/templates/prompts/` 에서 프롬프트 템플릿 수정
- `application.properties`에서 벡터 데이터베이스 설정 구성
- 새로운 MCP 서버 구성을 구현하여 추가 MCP 도구 추가
```

## 시스템 요구 사항

- CPU: 2 코어 이상
- 메모리: 4GB 이상
- 디스크: 10GB 이상의 여유 공간
- 네트워크: 인터넷 연결 필요 (OpenAI API 및 Brave 검색 API 사용)

## 보안 고려 사항

- 민감한 API 키는 환경 변수로 관리
- PostgreSQL MCP 도구는 보안상의 이유로 SELECT 쿼리만 허용
- 문서 접근은 인증된 사용자로 제한 권장

## 성능 최적화 팁

1. 큰 문서는 청크로 분할하여 처리
2. 벡터 검색 결과를 적절한 크기로 제한
3. 프롬프트 템플릿 최적화로 토큰 사용량 감소
4. 자주 사용되는 임베딩은 캐싱하여 API 호출 감소

## 향후 개선 방향

1. 더 많은 MCP 도구 통합 (이메일, 캘린더, CRM 등)
2. 사용자 인증 및 역할 기반 접근 제어
3. 실시간 데이터 처리 기능 강화
4. 모바일 앱 클라이언트 개발
