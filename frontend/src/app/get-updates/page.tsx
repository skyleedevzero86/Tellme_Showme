import { GetUpdatesPanel } from '@/presentation/components/GetUpdatesPanel';

export default function GetUpdatesPage() {
  return (
    <div className="chat-layout">
      <div className="chat-container">
        <header className="chat-header">
          Long Polling (getUpdates)
          <span className="chat-header-sub">주기적으로 새 메시지를 조회·처리합니다.</span>
        </header>
        <div className="chat-messages">
          <div className="card">
            <strong>안내</strong>
            <ul className="muted" style={{ marginTop: 8, paddingLeft: 18 }}>
              <li>텔레그램에서 봇을 대화상대로 추가합니다.</li>
              <li>getUpdates로 메시지를 가져오고, 백엔드가 처리/응답합니다.</li>
              <li>웹후크가 설정되어 있으면 폴링을 사용할 수 없습니다.</li>
            </ul>
          </div>
          <GetUpdatesPanel />
        </div>
      </div>
    </div>
  );
}
