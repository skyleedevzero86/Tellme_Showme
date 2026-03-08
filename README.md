
# Tellme Showme
<br/>
**Telegram Bot 관리·운영 플랫폼** — 웹후크/롱폴링 수신, 채널 브로드캐스트(메시지·이미지·파일), 봇 명령 처리(로또·성경·영어·검색 등)를 하나의 풀스택으로 제공합니다.
<br/>
<img width="1408" height="768" alt="image" src="https://github.com/user-attachments/assets/43f1512c-c3b6-4f83-94fb-702bd9ae4046" />
<br/>
---

## 프로젝트 소개

Tellme Showme는 [Telegram Bot API](https://core.telegram.org/bots/api)를 사용하는 **풀스택 봇 운영 플랫폼**입니다.<br/>
<br/>
- **백엔드**: Kotlin, Spring Boot 4.x, WebFlux(WebClient), Gradle. 유스케이스·포트·어댑터 구조.<br/>
- **프론트엔드**: TypeScript, Next.js 16 (App Router), React 19. 웹 UI에서 웹후크/롱폴링/채널 전송을 수행.<br/>
<br/>
웹에서 웹후크 설정·해제, Long Polling 모니터링, 채널로 **메시지·이미지·파일(문서)** 전송(캡션 지원)이 가능하며, 봇 명령(`/start`, `/time`, `/lotto`, `/god`, `/eng`, `/search`, `/doc`)으로 확장할 수 있게 구성되어 있습니다.
<br/>
---

## 기술 스택

| 구분 | 기술 |
|------|------|
| Backend | Java 21, Kotlin 2.2, Spring Boot 4.0, Spring WebFlux, Gradle |
| Frontend | Next.js 16, React 19, TypeScript, Tailwind CSS 4, pnpm |

---

## 실행 방법

### 백엔드 (포트 8080)

```bash
cd backend
./gradlew bootRun
```

### 프론트엔드 (포트 3000)

```bash
cd frontend
pnpm install
pnpm run dev
```

- **백엔드**: http://localhost:8080  
- **프론트엔드**: http://localhost:3000  
- 프론트엔드는 `NEXT_PUBLIC_API_URL` 없으면 `/api/proxy/*`로 백엔드 8080을 프록시하는 설정이 필요할 수 있습니다.

---

## 환경 변수 (백엔드)

| 변수 | 설명 | 비고 |
|------|------|------|
| `TELEGRAM_BOT_TOKEN` | 봇 토큰 | 운영 시 반드시 설정 권장 |
| `TELEGRAM_WEBHOOK_URL` | 웹후크 콜백 URL (예: ngrok) | 웹후크 사용 시 설정 |
| `TELEGRAM_CHANNEL_USERNAME` | 채널 사용자명 (예: `@studylkt`) | 채널 전송 시 사용, 기본값 있음 |
| `BIBLE_API_URL` | 성경 API URL | `/god` 등에서 사용 시 |
| `ENGLISH_API_URL` | 영어 API URL | `/eng` 등에서 사용 시 |

---

## 주요 기능 및 API

### 웹 UI 메뉴

- **웹후크 설정/해제** (`/webhook`) — setWebhook으로 콜백 URL 등록·삭제, 현재 등록 URL 확인
- **Long Polling** (`/get-updates`) — getUpdates 주기 조회 (웹후크 사용 중이면 안내)
- **채널 브로드캐스트** (`/channel`) — 보내기 유형(메시지 / 이미지 / 파일) 선택, 캡션(선택)과 함께 전송

### 백엔드 API (예시)

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/webhook_status.do` | 웹후크 설정 여부 |
| GET | `/webhook_info.do` | Telegram에 등록된 웹후크 URL 등 |
| GET | `/webhook_delete.do` | 웹후크 삭제 |
| GET | `/webHook.do` | setWebhook (enabled, url 쿼리) |
| GET | `/get_updates.do` | Long Polling 조회 |
| POST | `/send_message.do` | 채널에 메시지 전송 |
| POST | `/file_upload.do` | 채널에 이미지 전송 (multipart: filename, caption 선택) |
| POST | `/document_upload.do` | 채널에 파일(문서) 전송 (multipart: filename, caption 선택) |
| POST | `/callback.do` | 웹후크 콜백 (Telegram → 백엔드) |

---

## 프로젝트 구조

```
Tellme_Showme/
├── backend/                    # Spring Boot (Kotlin)
│   ├── src/main/kotlin/.../
│   │   ├── application/        # port, usecase
│   │   ├── domain/             # dto 등
│   │   └── infrastructure/    # web(Controller), adapter(Telegram, External)
│   └── src/main/resources/
│       └── application.yml
├── frontend/                   # Next.js (App Router)
│   ├── src/
│   │   ├── app/                # page.tsx (/, /webhook, /get-updates, /channel)
│   │   ├── application/hooks/ # useWebhook, useChannelBroadcast 등
│   │   ├── domain/             # constants, types
│   │   ├── infrastructure/api/ # TelegramApiRepository
│   │   └── presentation/       # WebhookPanel, ChannelBroadcastPanel 등
│   └── package.json
└── README.md
```

---

## 라이선스

LICENSE 파일을 참고하세요.
