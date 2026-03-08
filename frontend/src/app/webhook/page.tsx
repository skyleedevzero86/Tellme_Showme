import { WebhookPanel } from '@/presentation/components/WebhookPanel';

export default function WebhookPage() {
  return (
    <>
      <h2>텔레그램 봇 예제 (웹후크)</h2>
      <hr />
      <ul>
        <li>텔레그램에서 봇을 대화상대로 추가합니다.</li>
        <li>setWebhook으로 봇이 수신한 메시지를 청취할 URL을 설정합니다.</li>
        <li>봇 명령: /lotto, /god, /eng 등</li>
      </ul>
      <div style={{ marginTop: 12, padding: 12, background: '#f5f5f5', borderRadius: 8, fontSize: 14 }}>
        <strong>웹후크 vs 폴링</strong>
        <ul style={{ marginTop: 8, marginBottom: 0 }}>
          <li><strong>웹후크 설정</strong>: 서버에 <code>TELEGRAM_WEBHOOK_URL</code> 환경 변수로 <strong>HTTPS URL</strong>을 넣어야 합니다. (Telegram은 443, 80, 88, 8443 포트만 지원)</li>
          <li><strong>로컬 개발</strong>: localhost는 HTTPS가 아니므로 웹후크를 쓰기 어렵습니다. 백엔드의 <strong>폴링(getUpdates)</strong>을 사용하면 됩니다. <code>application.yml</code>에서 <code>polling.enabled: true</code>로 두면 주기적으로 메시지를 가져옵니다.</li>
        </ul>
      </div>
      <br />
      <WebhookPanel />
    </>
  );
}
