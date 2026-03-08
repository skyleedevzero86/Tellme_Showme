'use client';

import { useGetUpdates } from '@/application/hooks/useGetUpdates';

export function GetUpdatesPanel() {
  const { status, lastUpdated, isPolling, start, stop } = useGetUpdates();

  return (
    <section>
      <h3>getUpdates 수행 결과</h3>
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
