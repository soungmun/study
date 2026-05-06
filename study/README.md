# 공지사항 게시판 (Notice Board)

Spring Boot + React 기반 공지사항 CRUD 학습용 프로젝트입니다.
백엔드는 JPA와 MyBatis를 병행하며, 프론트엔드는 Vite + React로 구성되어 있습니다.

---

## 1. 프로젝트 소개

### 개요
공지사항을 **등록 / 조회 / 수정 / 삭제**할 수 있는 풀스택 게시판 애플리케이션입니다.
검색, 페이징, 조회수 카운트, 입력값 검증까지 실제 게시판에서 필요한 기본 기능을 다룹니다.

### 기술 스택

| 구분 | 기술 |
|------|------|
| Backend | Java 17, Spring Boot 4.0.6, Spring Data JPA, MyBatis 3.0.3, Spring Validation |
| Database | MariaDB |
| Frontend | React 19, React Router 7, Vite 8 |
| 기타 | Lombok, Gradle |

### 주요 기능
- 공지 **목록 조회** (작성자/제목 검색 + 페이지네이션)
- 공지 **상세 조회** (조회 시 viewCount 자동 증가)
- 공지 **등록 / 수정 / 삭제**
- 백엔드 입력값 검증 (`@Valid`, `@NotBlank`, `@Size`)
- 조회수 1,000 이상은 `1.2k` 형식으로 변환

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
│   ├── controller/   # REST API
│   ├── entity/       # Notice 엔티티
│   ├── repository/   # JPA Repository
│   ├── mapper/       # MyBatis Mapper
│   ├── service/      # 비즈니스 로직
│   └── config/       # CORS 등
├── src/main/resources/
│   ├── application.properties
│   └── mapper/       # MyBatis XML
└── frontend/src/components/
    ├── NoticeList.jsx
    ├── NoticeDetail.jsx
    ├── NoticeCreate.jsx
    └── NoticeEdit.jsx
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

---

## 2. API 문서

- **Base URL**: `http://localhost:8080`
- **공통 Content-Type**: `application/json`
- **공통 경로**: `/api/notices`

### 2.1 공지 목록 조회

```
GET /api/notices
```

#### Query Parameters
| 이름 | 타입 | 필수 | 기본값 | 설명 |
|------|------|------|--------|------|
| type | String | N | `title` | 검색 항목 (`title` / `author`) |
| keyword | String | N | - | 검색어 (없으면 전체 조회) |
| page | int | N | 0 | 페이지 번호 (0부터) |
| size | int | N | 10 | 페이지당 건수 |
| sort | String | N | `id,asc` | 정렬 조건 |

#### Request 예시
```
GET /api/notices?type=title&keyword=공지&page=0&size=10
```

#### Response 200 OK
```json
{
  "content": [
    {
      "id": 1,
      "author": "관리자",
      "title": "첫 번째 공지",
      "content": "내용입니다.",
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

---

### 2.2 공지 상세 조회

```
GET /api/notices/{id}
```

호출 시 해당 공지의 **조회수가 1 증가**합니다.

#### Path Parameters
| 이름 | 타입 | 설명 |
|------|------|------|
| id | Long | 공지 ID |

#### Response 200 OK
```json
{
  "id": 1,
  "author": "관리자",
  "title": "첫 번째 공지",
  "content": "내용입니다.",
  "createdAt": "2026-05-06 10:30:00",
  "viewCount": 13
}
```

#### Response 404
존재하지 않는 ID 요청 시 오류 응답 반환.

---

### 2.3 공지 등록

```
POST /api/notices
```

#### Request Body
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
| author | 필수, 100자 이하 | "작성자를 입력하세요." / "작성자는 100자 이하여야 합니다." |
| title | 필수, 200자 이하 | "제목을 입력하세요." / "제목은 200자 이하여야 합니다." |
| content | 필수 | "내용을 입력하세요." |

#### Response 200 OK
등록된 Notice 객체 반환 (id, createdAt 포함).

---

### 2.4 공지 수정

```
PUT /api/notices/{id}
```

#### Path Parameters
| 이름 | 타입 | 설명 |
|------|------|------|
| id | Long | 공지 ID |

#### Request Body
```json
{
  "author": "관리자",
  "title": "수정된 제목",
  "content": "수정된 내용"
}
```

검증 규칙은 등록과 동일합니다.

#### Response 200 OK
수정된 Notice 객체 반환.

---

### 2.5 공지 삭제

```
DELETE /api/notices/{id}
```

#### Path Parameters
| 이름 | 타입 | 설명 |
|------|------|------|
| id | Long | 공지 ID |

#### Response 200 OK
본문 없음.

---

### 2.6 오류 응답 형식

Spring Boot 기본 오류 응답을 따릅니다 (`server.error.include-message=always`,
`server.error.include-binding-errors=always`).

```json
{
  "timestamp": "2026-05-06T10:30:00.000+00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "제목을 입력하세요.",
  "path": "/api/notices"
}
```

| HTTP | 상황 |
|------|------|
| 400 | 입력값 검증 실패 |
| 404 | 존재하지 않는 공지 ID |
| 500 | 서버 내부 오류 |