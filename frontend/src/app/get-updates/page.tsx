import { GetUpdatesPanel } from '@/presentation/components/GetUpdatesPanel';

export default function GetUpdatesPage() {
  return (
    <>
      <h2>텔레그램 봇 예제 (Long Polling)</h2>
      <hr />
      <ul>
        <li>텔레그램에서 봇을 대화상대로 추가합니다.</li>
        <li>getUpdates와 sendMessage를 사용합니다.</li>
        <li>시작 버튼으로 주기적으로 새 메시지를 조회·처리합니다.</li>
        <li>봇 명령: /time, /lotto 등</li>
      </ul>
      <br />
      <GetUpdatesPanel />
    </>
  );
}
