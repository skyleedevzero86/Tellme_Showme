'use client';

import Link from 'next/link';
import { useGetUpdates } from '@/application/hooks/useGetUpdates';

export function GetUpdatesPanel() {
  const { status, lastUpdated, isPolling, start, stop, lastWebhookActive } = useGetUpdates();

  return (
    <section>
      <h3>getUpdates 수행 결과</h3>
      {lastWebhookActive && (
        <p style={{ marginBottom: 8, padding: 8, background: '#fff3cd', borderRadius: 4, fontSize: 14 }}>
          웹후크가 설정되어 있어 폴링(getUpdates)을 사용할 수 없습니다. <Link href="/webhook">웹후크 페이지</Link>에서 「Telegram에서만 삭제」 후 다시 시도하세요.
        </p>
      )}
      <p>마지막 업데이트 시간: {lastUpdated?.toLocaleString() ?? '-'}</p>
      <hr />
      <div>
        <button
          type="button"
          onClick={start}
          disabled={isPolling}
        >
          시작
        </button>
        <button
          type="button"
          onClick={stop}
          disabled={!isPolling}
        >
          종료
        </button>
      </div>
      <br />
      <div data-testid="status">{status || '(대기 중)'}</div>
    </section>
  );
}
