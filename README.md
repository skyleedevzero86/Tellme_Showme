# Tellme Showme

- **프론트엔드**: TypeScript, Next.js 16, React 18<br/>
---
- http://localhost:8080/index.html — 웹후크 설정/해제
LICENSE 파일 참고.


**Telegram Bot 관리·운영 플랫폼** — 웹후크/롱폴링 수신, 채널 브로드캐스트, 명령 처리(로또·성경·영어·검색 등)를 하나의 풀스택으로 제공합니다.<br/>
<img width="1408" height="768" alt="image" src="https://github.com/user-attachments/assets/43f1512c-c3b6-4f83-94fb-702bd9ae4046" />
<br/>
## 프로젝트 소개<br/>
Tellme Showme는 [Telegram Bot API](https://core.telegram.org/bots/api)를 활용한 **풀스택 봇 운영 플랫폼**입니다.  <br/>
백엔드는 **Spring Boot 4.x** 로 유스케이스·포트·어댑터를 명확히 나누었고, 프론트엔드는 **Next.js 14(App Router) 스타일 레이어**로 구성했습니다.  <br/>
<br/>
웹후크 설정/해제, Long Polling 모니터링, 채널 메시지·이미지 전송을 웹 UI에서 수행할 수 있으며, 봇 명령(/start, /time, /lotto, /god, /eng,/search,/doc)으로 확장 가능하게 구현했습니다.<br/>
<br/>
- **백엔드**: Java 21, Spring Boot 4.2, WebFlux(WebClient), gradle<br/>