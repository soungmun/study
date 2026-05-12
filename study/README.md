# 게시판 + 부가 기능 (Spring Boot + React)

Spring Boot 백엔드 + React 프론트엔드로 만든 학습용 풀스택 프로젝트.
공지사항 게시판을 중심으로, 회원 인증과 외부 API(책/지도/날씨/미세먼지) 기능이 붙어 있습니다.

---

## 1. 프로젝트 소개

### 개요

기능을 도메인별로 묶으면 다음과 같습니다.

**회원 / 인증**
- 일반 가입·로그인 (아이디/비밀번호)
- 카카오 OAuth2 로그인 (Authorization Code, 서버 사이드)
- 회원정보 수정 (`/me/edit`): 닉네임·이메일·비밀번호 변경
- 비밀번호 찾기 / 재설정 (이메일 토큰, 30분 유효)
- 가입·비밀번호 변경 시 알림 메일 자동 발송 (네이버/Gmail SMTP)

**공지사항(Notice)**
- CRUD, 검색(제목/작성자/내용), 페이지네이션
- 조회수 카운트 (상세 조회와 분리된 `POST /api/notices/{id}/views`)

**외부 API 프록시**
- 카카오 책 검색
- 카카오 로컬 키워드 검색 + Kakao Maps SDK (지도 마커, 클릭 좌표 역지오코딩)
- 선택 좌표의 현재 날씨 (Open-Meteo)
- 선택 좌표의 미세먼지 PM10/PM2.5/AQI (Open-Meteo Air Quality)

**관리**
- 회원 목록 조회 (로그인 필요)
- 공지 메일 단체 발송 (관리자 전용, 수신 동의한 회원에게만 BCC 발송)
- 매일 저녁 8시(KST) 자동 메일 — 날짜 인사말 + 국내 주요 지수·종목 시세(Yahoo Finance) 표를 수신 동의자에게 자동 발송 (동의자 0명이면 스킵)
  - 종목: 코스피, 코스닥, 삼성전자, SK하이닉스, NAVER, 카카오
  - 자동 메일의 날씨/미세먼지는 **내일 기준** 예보 (Open-Meteo daily/hourly)

**결제**
- 카카오페이 단건결제 (`/pay`) — 테스트 CID(`TC0ONETIME`) 기본, 운영 시 환경변수로 실 CID/Secret Key 주입
- 결제 준비 → 카카오 결제창 redirect → 승인/취소/실패 콜백 처리
- `Payment` 엔티티에 주문/승인 결과 영속화 (status: READY/APPROVED/CANCELED/FAILED)

### 기술 스택

| 구분 | 기술 |
|------|------|
| Backend | Java 17, Spring Boot 4.0.6, Spring Data JPA, MyBatis 3.0.3, Spring Validation, RestClient, springdoc-openapi (Swagger), Spring Mail |
| Database | MariaDB |
| Frontend | React 19, React Router 7, Vite 8 |
| 외부 API | Kakao 책 검색, Kakao 로컬, Kakao Maps JS SDK, Kakao OAuth2, Open-Meteo, Open-Meteo Air Quality |
| 기타 | Lombok, Gradle, BCrypt |

### 도메인 모델

#### Notice

| 필드 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | Long | PK, AUTO_INCREMENT | 식별자 |
| author | String(100) | NOT NULL | 작성자 |
| title | String(200) | NOT NULL | 제목 |
| content | TEXT | NOT NULL | 내용 |
| createdAt | LocalDateTime | NOT NULL | 생성 시각 (자동) |
| viewCount | long | NOT NULL, default 0 | 조회수 |

#### User

| 필드 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | Long | PK, AUTO_INCREMENT | 식별자 |
| kakaoId | Long | UNIQUE | 카카오 사용자 ID |
| username | String(50) | UNIQUE | 일반 가입 아이디 |
| password | String(100) | - | BCrypt 해시 (카카오 가입자는 null) |
| nickname | String(100) | - | 표시명 |
| profileImage | String(500) | - | 카카오 프로필 이미지 URL |
| email | String(200) | - | 이메일 |
| notificationOptIn | boolean | NOT NULL, default false | 공지 메일 수신 동의 여부 |
| createdAt / updatedAt | LocalDateTime | NOT NULL | 자동 관리 |
| resetToken / resetTokenExpiresAt | String / LocalDateTime | - | 비밀번호 재설정 토큰 |

### 디렉터리 구조

```
study/
├── src/main/java/com/example/study/
│   ├── controller/        # REST API
│   │   ├── AuthController.java          # 로그인/로그아웃/me/카카오
│   │   ├── UserAccountController.java   # 가입/비번찾기/프로필수정
│   │   ├── UserAdminController.java     # 회원 목록
│   │   ├── NoticeController.java
│   │   ├── BookApiController.java
│   │   ├── PlaceApiController.java
│   │   ├── WeatherApiController.java
│   │   ├── AirQualityApiController.java
│   │   └── GlobalExceptionHandler.java
│   ├── dto/               # 요청/응답 DTO
│   ├── entity/            # Notice, User
│   ├── repository/        # JPA Repository
│   ├── mapper/            # MyBatis Mapper
│   ├── service/           # AuthService, KakaoOAuthService, EmailService,
│   │                      # NoticeService, BookService, PlaceService,
│   │                      # WeatherService, AirQualityService
│   └── config/            # CORS, OpenAPI
├── src/main/resources/
│   ├── application.properties
│   └── mapper/            # MyBatis XML
└── frontend/src/
    ├── App.jsx            # 라우팅 + 네비게이션
    └── components/
        ├── AuthBar.jsx, RequireAuth.jsx
        ├── ProfileEdit.jsx
        ├── PasswordForgot.jsx, PasswordReset.jsx
        ├── NoticeList/Detail/Create/Edit.jsx
        ├── BookSearch.jsx
        ├── MapSearch.jsx
        └── UserList.jsx
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

- **Swagger UI**: `http://localhost:8080/swagger-ui/index.html`
- **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`

### 환경 설정

`src/main/resources/application.properties` 주요 항목:

| 키 | 설명 |
|---|---|
| `spring.datasource.*` | MariaDB 접속 정보 |
| `kakao.api.key` | 카카오 REST API Key (책/장소 검색용) |
| `kakao.oauth.client-id`, `client-secret`, `redirect-uri` | 카카오 OAuth 설정 |
| `app.frontend.url` | 카카오 콜백 후 리다이렉트할 프론트 URL |
| `spring.mail.*` | SMTP 설정 (기본: 네이버) |
| `app.admin.username` | 단체 메일 발송 권한을 가진 관리자 username (기본: `admin`, 환경변수 `ADMIN_USERNAME`) |
| `kakaopay.cid` | 카카오페이 가맹점 코드 (테스트 기본 `TC0ONETIME`, 환경변수 `KAKAOPAY_CID`) |
| `kakaopay.secret-key` | 카카오페이 Secret Key (`KAKAOPAY_SECRET_KEY`로 주입, 미설정 시 결제 시도 시 500) |
| `kakaopay.api-base` | 카카오페이 Open API 베이스 URL (기본 `https://open-api.kakaopay.com`) |
| `kakaopay.approval-url` / `cancel-url` / `fail-url` | Kakao가 호출할 서버 콜백 URL (기본 localhost:8080) |

**SMTP 환경변수 전환** (네이버 ↔ Gmail):

- 네이버 (기본): `MAIL_USERNAME=<id>@naver.com`, `MAIL_PASSWORD=<로그인 비밀번호>`
  *(네이버 메일 환경설정 → POP3/IMAP → SMTP 사용함 체크)*
- Gmail: `MAIL_HOST=smtp.gmail.com`, `MAIL_PORT=587`, `MAIL_SSL_ENABLE=false`, `MAIL_STARTTLS=true`, `MAIL_USERNAME=<id>@gmail.com`, `MAIL_PASSWORD=<앱 비밀번호 16자>`

`frontend/index.html`
- Kakao Maps **JavaScript Key**가 SDK 스크립트에 하드코딩 (`appkey=...`)

> ⚠️ **카카오 개발자 콘솔 활성화 필요**
> - 책 검색: 별도 활성화 불필요
> - 장소/지도: **앱 설정 → 제품 설정 → 지도/로컬** 활성화
> - 카카오 로그인: **카카오 로그인** 활성화 + Redirect URI에 `http://localhost:8080/api/auth/kakao/callback` 등록 + 동의항목(닉네임/이메일/프로필사진)
> - **앱 설정 → 플랫폼 → Web 도메인**에 `http://localhost:5173` 등록
> - **카카오페이**: [Kakao Pay 개발자 센터](https://developers.kakaopay.com/)에서 별도 가맹점 등록 후 Secret Key 발급 → `KAKAOPAY_SECRET_KEY` 환경변수로 주입. CID 미설정 시 테스트 CID(`TC0ONETIME`)로 동작.

---

## 2. API 문서

- **Base URL**: `http://localhost:8080`
- **세션 쿠키**: `JSESSIONID` (HttpOnly, SameSite=lax). 프론트는 `credentials: 'include'`로 호출
- **공통 Content-Type**: `application/json` (form 인코딩 사용처는 명시)

### 2.1 Auth API (`/api/auth`)

| Method | Path | 인증 | 설명 |
|--------|------|------|------|
| POST | `/api/auth/signup` | - | 회원가입 (가입 후 자동 로그인) |
| POST | `/api/auth/login` | - | 로그인 |
| POST | `/api/auth/logout` | - | 로그아웃 (세션 무효화) |
| GET  | `/api/auth/me` | 세션 | 현재 로그인 사용자 정보 (미로그인 시 401) |
| PUT  | `/api/auth/me` | 세션 | 닉네임/이메일/비밀번호 수정 |
| POST | `/api/auth/password/forgot` | - | 재설정 토큰 메일 발송 (이메일 존재 여부와 무관하게 200) |
| POST | `/api/auth/password/reset` | - | 토큰으로 비밀번호 재설정 |
| GET  | `/api/auth/kakao/login` | - | 카카오 인가 페이지로 리다이렉트 (`returnTo` 파라미터로 복귀 URL 지정) |
| GET  | `/api/auth/kakao/callback` | - | 카카오 콜백 (인가 코드 → 토큰 → 사용자 → 세션 등록) |

#### 회원가입 요청
```json
{ "username": "alice", "password": "secret123", "nickname": "앨리스", "email": "alice@example.com" }
```

#### 로그인 요청
```json
{ "username": "alice", "password": "secret123" }
```

#### `/api/auth/me` 응답 (200)
```json
{ "id": 1, "kakaoId": null, "username": "alice", "nickname": "앨리스",
  "profileImage": null, "email": "alice@example.com" }
```

#### 프로필 수정 요청 (`PUT /api/auth/me`)
```json
{ "nickname": "새닉", "email": "new@example.com",
  "currentPassword": "secret123", "newPassword": "newSecret456" }
```
- 카카오 가입자(아이디 없음)는 비밀번호 변경 불가 → `400 KAKAO_ONLY`
- 일반 가입자가 `currentPassword` 틀리면 → `400 CURRENT_PASSWORD_MISMATCH`

### 2.2 Notice API (`/api/notices`)

| Method | Path | 설명 |
|--------|------|------|
| GET    | `/api/notices?type=&keyword=&page=&size=&sort=` | 목록 (검색 + 페이징) |
| GET    | `/api/notices/{id}` | 상세 (조회수 증가 없음) |
| POST   | `/api/notices/{id}/views` | 조회수 +1 (반환은 갱신된 Notice) |
| POST   | `/api/notices` | 등록 |
| PUT    | `/api/notices/{id}` | 수정 |
| DELETE | `/api/notices/{id}` | 삭제 |

> 상세 조회와 조회수 증가는 분리되어 있어, 프론트에서 조회수만 따로 호출합니다.

#### 목록 응답 예시
```json
{
  "content": [
    { "id": 1, "author": "관리자", "title": "첫 공지", "content": "내용",
      "createdAt": "2026-05-06 10:30:00", "viewCount": 12 }
  ],
  "totalElements": 1, "totalPages": 1, "number": 0, "size": 10, "first": true, "last": true
}
```

#### 등록/수정 요청
```json
{ "author": "관리자", "title": "공지 제목", "content": "공지 내용입니다." }
```

#### 검증 규칙
| 필드 | 규칙 |
|---|---|
| author | 필수, 100자 이하 |
| title | 필수, 200자 이하 |
| content | 필수 |

### 2.3 외부 API 프록시

#### Book — `GET /api/books/search`
| 파라미터 | 필수 | 기본 | 설명 |
|---|---|---|---|
| query | Y | - | 검색어 |
| target | N | (전체) | `title` / `person` / `publisher` / `isbn` |
| sort | N | `accuracy` | `accuracy` / `latest` |
| page | N | 1 | 1~50 |
| size | N | 10 | 1~50 |

#### Place — `GET /api/places/search`
| 파라미터 | 필수 | 기본 | 설명 |
|---|---|---|---|
| query | Y | - | 검색어 |
| page | N | 1 | 1~45 |
| size | N | 15 | 1~15 |

각 결과의 `x`(경도)/`y`(위도)를 이용해 프론트 Kakao Maps SDK가 마커 표시.

#### Weather — `GET /api/weather?lat={lat}&lng={lng}`
선택 좌표의 현재 기온/풍속/풍향/날씨 코드 (Open-Meteo).

#### Air Quality — `GET /api/air?lat={lat}&lng={lng}`
선택 좌표의 PM10 / PM2.5 / 유럽 AQI 등급 (Open-Meteo Air Quality).

### 2.4 Admin API

| Method | Path | 인증 | 설명 |
|---|---|---|---|
| GET  | `/api/users` | 세션 | 가입한 회원 목록 (최신순) |
| GET  | `/api/admin/me` | 세션 | 현재 사용자가 관리자인지 + 수신 동의자 수 |
| POST | `/api/admin/broadcast` | 관리자 | 수신 동의한 회원 전체에게 공지 메일 BCC 발송 |

#### 발송 요청 (`POST /api/admin/broadcast`)
```json
{ "subject": "5월 시스템 점검 안내", "body": "안녕하세요...\n자세한 내용은..." }
```
- 본문 줄바꿈은 `<br>`로 변환
- `app.admin.username`과 일치하는 username으로 로그인한 사용자만 호출 가능 (그 외 403)
- 수신자: `User.notificationOptIn = true`인 회원의 이메일

### 2.5 오류 응답

`GlobalExceptionHandler`가 통일된 형식으로 반환합니다.

```json
{ "message": "오류 메시지" }
```

| HTTP | 상황 |
|------|------|
| 400 | 입력값 검증 실패 / 비즈니스 규칙 위반 |
| 401 | 미로그인 / 인증 실패 |
| 404 | 리소스 없음 |
| 409 | 아이디 중복 등 충돌 |
| 500 | 서버 내부 오류 (외부 API 실패 포함) |

---

## 3. 프론트엔드 화면

상단 네비게이션: **공지사항 / 책 검색 / 지도** + 우측 AuthBar(로그인 상태/버튼).
*(회원 목록 `/users`는 네비에서 제외 — URL 직접 접근만 가능)*

| 경로 | 화면 | 인증 |
|------|------|------|
| `/` | 공지사항 목록 | 필요 |
| `/notices/new` | 공지 등록 | 필요 |
| `/notices/:id` | 공지 상세 (조회수 +1 호출) | 필요 |
| `/notices/:id/edit` | 공지 수정 | 필요 |
| `/books` | 카카오 책 검색 | 필요 |
| `/map` | 장소 검색 + 지도 (날씨/미세먼지 패널) | 필요 |
| `/users` | 회원 목록 | 공개 (백엔드는 세션 요구) |
| `/me/edit` | 회원정보 수정 | 필요 |
| `/forgot` | 비밀번호 찾기 (재설정 메일 요청) | 공개 |
| `/reset?token=...` | 비밀번호 재설정 (토큰 검증) | 공개 |
| `/admin/broadcast` | 공지 메일 발송 (수신 동의자 대상) | 관리자만 |

> 인증이 필요한 라우트는 `RequireAuth`로 감싸 세션 확인 후 진입. 미로그인 시 안내 화면 노출.