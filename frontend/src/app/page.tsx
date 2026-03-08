import Link from 'next/link';

export default function HomePage() {
  return (
    <>
      <h1>텔레그램 봇 예제</h1>
      <hr />
      <ul>
        <li>텔레그램에서 봇을 대화상대로 추가합니다.</li>
        <li>Bot API의 setWebhook / getUpdates / sendMessage를 사용합니다.</li>
        <li>웹후크 또는 Long Polling으로 메시지를 수신하고 응답합니다.</li>
        <li>봇 명령: /start, /time, /lotto, /god, /eng</li>
      </ul>
      <h3>메뉴</h3>
      <ul>
        <li>
          <Link href="/webhook">웹후크 설정/해제</Link> — setWebhook으로 콜백 URL 등록
        </li>
        <li>
          <Link href="/get-updates">Long Polling</Link> — get_updates.do로 주기 조회
        </li>
        <li>
          <Link href="/channel">채널 브로드캐스트</Link> — 메시지·이미지 전송
        </li>
      </ul>
    </>
  );
}
