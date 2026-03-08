'use client';

import { useWebhook } from '@/application/hooks/useWebhook';

export function WebhookPanel() {
  const { setWebhook, status, result, error } = useWebhook();

  const handleSet = () => setWebhook(true).catch(() => {});
  const handleUnset = () => setWebhook(false).catch(() => {});

  return (
    <section>
      <h3>웹후크 설정 및 해제하기</h3>
      <hr />
      <div>
        <button type="button" onClick={handleSet} disabled={status === 'loading'}>
          설정
        </button>
        <button type="button" onClick={handleUnset} disabled={status === 'loading'}>
          해제
        </button>
      </div>
      <br />
      <h3>결과</h3>
      <hr />
      {error && <p style={{ color: 'red' }}>{error}</p>}
      {result != null && (
        <pre style={{ fontSize: 12 }}>{JSON.stringify(result, null, 2)}</pre>
      )}
    </section>
  );
}
