import { WebhookPanel } from '@/presentation/components/WebhookPanel';

export default function WebhookPage() {
  return (
    <div className="chat-layout">
      <div className="chat-container">
        <header className="chat-header">
          웹후크 설정
          <span className="chat-header-sub">ngrok HTTPS URL을 등록/해제하고 상태를 확인합니다.</span>
        </header>
        <div className="chat-messages">
          <div className="card">
            <strong>웹후크 vs 폴링</strong>
            <ul className="muted" style={{ marginTop: 8, paddingLeft: 18 }}>
              <li>
                <strong>웹후크</strong>: 서버에 <code>TELEGRAM_WEBHOOK_URL</code>로 <strong>HTTPS URL</strong>을 넣어야 합니다.
                (Telegram은 443, 80, 88, 8443 포트만 지원)
              </li>
              <li>
                <strong>로컬 개발</strong>: localhost는 HTTPS가 아니라 웹후크가 어렵습니다. 대신 <strong>폴링(getUpdates)</strong>을 사용하세요.
                <code>application.yml</code>에서 <code>polling.enabled: true</code>.
              </li>
            </ul>
          </div>
          <WebhookPanel />
        </div>
      </div>
    </div>
  );
}
