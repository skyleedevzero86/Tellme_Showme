'use client';

import Link from 'next/link';
import { useGetUpdates } from '@/application/hooks/useGetUpdates';

type GetUpdatesPanelProps = {
  onStarted?: () => void;
};

export function GetUpdatesPanel({ onStarted }: GetUpdatesPanelProps) {
  const { status, lastUpdated, isPolling, start, stop, lastWebhookActive } = useGetUpdates();

  return (
    <section className="stack" style={{ marginTop: 12 }}>
      {lastWebhookActive && (
        <p className="notice notice-warning">
          웹후크가 설정되어 있어 폴링(getUpdates)을 사용할 수 없습니다.{' '}
          <Link href="/webhook">웹후크 페이지</Link>에서 Telegram 웹후크를 해제한 뒤 다시 시도해 주세요.
        </p>
      )}
      <div className="row">
        <div className="muted" style={{ fontSize: 13 }}>
          마지막 업데이트 시간: {lastUpdated?.toLocaleString() ?? '-'}
        </div>
      </div>
      <div className="row">
        <button
          type="button"
          onClick={() => {
            start();
            onStarted?.();
          }}
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
      <div className="card" data-testid="status">
        {status || '(대기 중)'}
      </div>
    </section>
  );
}
