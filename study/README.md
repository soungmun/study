# 공지사항 + 책 검색 (Spring Boot + React)

Spring Boot 백엔드 + React 프론트엔드로 만든 학습용 풀스택 프로젝트입니다.
공지사항 CRUD와 카카오 책 검색 기능을 제공합니다.

---

## 1. 프로젝트 소개

### 개요
- **공지사항 게시판**: 등록 / 조회 / 수정 / 삭제, 검색, 페이지네이션, 조회수 카운트
- **책 검색**: 카카오 도서 검색 API 프록시 (제목/저자/출판사/ISBN 검색, 정렬, 페이징)
- **장소 검색 + 지도**: 카카오 키워드 검색 + Kakao Maps SDK로 지도 표시
  - 결과 리스트 ↔ 지도 마커 ↔ 우측 상세 패널 3-way 연동
  - **지도 클릭으로 임의 좌표 마커 추가** → coord2Address 역지오코딩으로 주소/건물명 자동 표시
  - 첫 진입 시 기본 위치(서울 시청)에 마커 자동 배치
- **카카오 로그인**: OAuth2 Authorization Code 흐름 (서버 사이드)
  - 백엔드가 인가 코드 ↔ 토큰 ↔ 사용자 정보 교환을 처리, 결과를 `users` 테이블에 upsert
  - Spring `HttpSession` 기반 로그인 상태 유지 (`/api/auth/me`, `/api/auth/logout`)
  - 프론트 상단 nav 바에 카카오 로그인 버튼 / 프로필·닉네임 표시

### 기술 스택

| 구분 | 기술 |
|------|------|
| Backend | Java 17, Spring Boot 4.0.6, Spring Data JPA, MyBatis 3.0.3, Spring Validation, RestClient, springdoc-openapi (Swagger) |
| Database | MariaDB |
| Frontend | React 19, React Router 7, Vite 8 |
| 외부 API | Kakao 책 검색 (`/v3/search/book`), 카카오 로컬 키워드 검색 (`/v2/local/search/keyword.json`), Kakao Maps JavaScript SDK |
| 기타 | Lombok, Gradle |

### 도메인 모델 — Notice

| 필드 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | Long | PK, AUTO_INCREMENT | 식별자 |
| author | String(100) | NOT NULL | 작성자 |
| title | String(200) | NOT NULL | 제목 |
| content | TEXT | NOT NULL | 내용 |
| createdAt | LocalDateTime | NOT NULL | 생성 시각 (자동) |
| viewCount | long | NOT NULL, default 0 | 조회수 |

### 디렉터리 구조
```
study/
├── src/main/java/com/example/study/
│   ├── controller/     # REST API
│   │   ├── NoticeController.java
│   │   ├── BookApiController.java
│   │   ├── PlaceApiController.java
│   │   └── GlobalExceptionHandler.java
│   ├── dto/            # 외부 API DTO
│   │   ├── BookSearchResponse.java
│   │   └── PlaceSearchResponse.java
│   ├── entity/         # Notice 엔티티
│   ├── repository/     # JPA Repository
│   ├── mapper/         # MyBatis Mapper
│   ├── service/        # NoticeService, BookService, PlaceService
│   └── config/         # CORS, OpenAPI 등
├── src/main/resources/
│   ├── application.properties
│   └── mapper/         # MyBatis XML
└── frontend/src/
    ├── App.jsx         # Routing + 네비게이션
    ├── App.css
    └── components/
        ├── NoticeList.jsx
        ├── NoticeDetail.jsx
        ├── NoticeCreate.jsx
        ├── NoticeEdit.jsx
        ├── BookSearch.jsx
        └── MapSearch.jsx
```

### 실행 방법

```bash
# 1. DB 준비 (MariaDB)
CREATE DATABASE notice DEFAULT CHARACTER SET utf8mb4;

# 2. 백엔드 (기본 포트 8080)
./gradlew bootRun

# 3. 프론트엔드 (기본 포트 5173)
cd frontend
npm install
npm run dev
```

브라우저에서 `http://localhost:5173` 접속.

**Swagger UI**: `http://localhost:8080/swagger-ui/index.html`
**OpenAPI JSON**: `http://localhost:8080/v3/api-docs`

### 환경 설정
`src/main/resources/application.properties`
- `spring.datasource.*` — MariaDB 접속 정보
- `kakao.api.key` — 카카오 REST API Key (책 검색 / 장소 검색에 사용)

`frontend/index.html`
- Kakao Maps **JavaScript Key**가 SDK 스크립트에 하드코딩되어 있음 (`appkey=...`)

> ⚠️ **카카오 개발자 콘솔에서 활성화 필요**
> - 책 검색: 별도 활성화 불필요
> - 장소/지도: **앱 설정 → 제품 설정 → 지도/로컬** 활성화
> - **앱 설정 → 플랫폼 → Web 도메인**에 `http://localhost:5173` 등록

---

## 2. API 문서

- **Base URL**: `http://localhost:8080`
- **공통 Content-Type**: `application/json`

### 2.1 Notice API (`/api/notices`)

| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/notices?type=&keyword=&page=&size=&sort=` | 목록 조회 (검색 + 페이징) |
| GET | `/api/notices/{id}` | 상세 조회 (조회수 +1) |
| POST | `/api/notices` | 등록 |
| PUT | `/api/notices/{id}` | 수정 |
| DELETE | `/api/notices/{id}` | 삭제 |

#### 목록 응답 예시
```json
{
  "content": [
    {
      "id": 1,
      "author": "관리자",
      "title": "첫 번째 공지",
      "content": "내용",
      "createdAt": "2026-05-06 10:30:00",
      "viewCount": 12
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "number": 0,
  "size": 10,
  "first": true,
  "last": true
}
```

#### 등록/수정 요청 예시
```json
{
  "author": "관리자",
  "title": "공지 제목",
  "content": "공지 내용입니다."
}
```

#### 검증 규칙
| 필드 | 규칙 | 오류 메시지 |
|------|------|------------|
| author | 필수, 100자 이하 | "작성자를 입력하세요." |
| title | 필수, 200자 이하 | "제목을 입력하세요." |
| content | 필수 | "내용을 입력하세요." |

---

### 2.2 Book API (`/api/books`)

카카오 책 검색 API 프록시. 호출 시 서버가 카카오에 요청을 보내고 결과를 그대로 반환합니다.

```
GET /api/books/search
```

#### Query Parameters
| 이름 | 타입 | 필수 | 기본값 | 설명 |
|------|------|------|--------|------|
| query | String | **Y** | - | 검색어 |
| target | String | N | (전체) | `title` / `person` / `publisher` / `isbn` |
| sort | String | N | `accuracy` | `accuracy`(정확도) / `latest`(최신) |
| page | int | N | 1 | 페이지 (1~50) |
| size | int | N | 10 | 페이지당 건수 (1~50) |

#### Request 예시
```
GET /api/books/search?query=spring%20boot&sort=accuracy&page=1&size=10
```

#### Response 200 OK
```json
{
  "meta": {
    "total_count": 87,
    "pageable_count": 87,
    "is_end": false
  },
  "documents": [
    {
      "title": "Boot Spring Boot!",
      "contents": "스프링 부트 입문서…",
      "url": "https://search.daum.net/search?w=bookpage&bookId=...",
      "isbn": "...",
      "datetime": "2018-08-31T00:00:00.000+09:00",
      "authors": ["저자"],
      "publisher": "출판사",
      "translators": [],
      "price": 25000,
      "sale_price": 22500,
      "thumbnail": "https://search1.kakaocdn.net/...",
      "status": "정상판매"
    }
  ]
}
```

---

### 2.3 Place API (`/api/places`)

카카오 로컬 키워드 검색 프록시.

```
GET /api/places/search
```

| 이름 | 타입 | 필수 | 기본값 | 설명 |
|------|------|------|--------|------|
| query | String | **Y** | - | 검색어 (장소명/주소/키워드) |
| page | int | N | 1 | 페이지 (1~45) |
| size | int | N | 15 | 페이지당 건수 (1~15) |

각 결과의 `x`(경도), `y`(위도)를 사용해 프론트의 Kakao Maps SDK가 마커를 찍습니다.

---

### 2.4 오류 응답

`GlobalExceptionHandler`가 통일된 형식으로 반환합니다.

```json
{ "message": "오류 메시지" }
```

| HTTP | 상황 |
|------|------|
| 400 | 입력값 검증 실패 (필드 오류 메시지를 줄바꿈으로 결합) |
| 404 | 존재하지 않는 리소스 |
| 500 | 서버 내부 오류 (외부 API 실패 등) |

---

## 3. 프론트엔드 화면

| 경로 | 화면 |
|------|------|
| `/` | 공지사항 목록 |
| `/notices/new` | 공지 등록 |
| `/notices/:id` | 공지 상세 |
| `/notices/:id/edit` | 공지 수정 |
| `/books` | 카카오 책 검색 |

상단 네비게이션에서 **공지사항 / 책 검색** 사이를 전환할 수 있습니다.