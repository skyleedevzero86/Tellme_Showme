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
      <br />
      <WebhookPanel />
    </>
  );
}
