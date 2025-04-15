# MCP 애플리케이션

스프링 부트 애플리케이션(MCPAPP)은 OpenAI API와 MCP(Model Context Protocol)를 를 구현한 Spring Boot 애플리케이션

## 아키텍처
현재 애플리케이션은 처리 흐름:

```
    [사용자]
       ↓
[Spring Boot App] 
       ↓
[MCP Client] ← OPENAI AI 가 Filesystem Tool 사용
       ↓
[Filesystem MCP 서버] ← 요청에 따른 파일 시스템 작업
       ↓
[사용자에게 응답 반환]
```
---
## 기능
1. OpenAI 모델(기본: gpt-4o-mini) 통합
2. MCP를 통한 파일 시스템 작업
    -  파일 읽기(read_file)
    -  파일 쓰기(write_file)
    - 디렉토리 목록 조회(list_directory)
    - 디렉토리 트리 보기(directory_tree)
    - 파일 검색(search_files)
3. 요청 타임아웃 및 로깅 설정 가능
4. SSE 기반 MCP 서버 통신

---
## 필수 조건
Java 17 이상

Gradle

OpenAI API 키

Docker DeskTop

---
## MCP 도구 통합
애플리케이션 MCP 도구통합

1. **파일시스템 MCP 도구**: 파일 읽기 및 쓰기 기능

<생성 예정>
2. **문서 처리 MCP 도구**: PDF,Excel 읽기 및 데이터 분석 추출
3. **데이터 시각화 MCP 도구**: 전처리된 데이터를 라이브러리 기반으로 시각화 데이터 추출 기능

---
### 사전 요구 사항
- JDK 17 이상
- Docker 및 Docker Compose
- OpenAI API 키
- Brave 검색 API 키 (선택사항)

---
### 환경 변수

다음 환경 변수를 설정하세요:
- `OPEN_API_KEY`: OpenAI API 키