'use client';

import { useCallback, useEffect, useState } from 'react';
import { useWebhook } from '@/application/hooks/useWebhook';
import { telegramApiRepository } from '@/infrastructure/api/TelegramApiRepository';
import type { WebhookInfoResponse } from '@/domain/types/api';

type StatusState = { kind: 'loading' } | { kind: 'ok'; configured: boolean; url: string } | { kind: 'error' };

export function WebhookPanel() {
  const { setWebhook, status, result, error } = useWebhook();
  const [statusState, setStatusState] = useState<StatusState>({ kind: 'loading' });
  const [webhookInfo, setWebhookInfo] = useState<WebhookInfoResponse | null>(null);
  const [webhookUrlInput, setWebhookUrlInput] = useState('');
  const [deleteLoading, setDeleteLoading] = useState(false);

  const fetchStatusAndInfo = useCallback(() => {
    setStatusState((s) => (s.kind === 'loading' ? s : { kind: 'loading' }));
    Promise.all([
      telegramApiRepository.getWebhookStatus(),
      telegramApiRepository.getWebhookInfo().catch(() => ({ ok: false, result: null })),
    ])
      .then(([r, info]) => {
        setStatusState({ kind: 'ok', configured: r.webhookUrlConfigured, url: r.webhookUrl ?? '' });
        setWebhookInfo(info);
      })
      .catch(() => setStatusState({ kind: 'error' }));
  }, []);

  useEffect(() => {
    fetchStatusAndInfo();
  }, [fetchStatusAndInfo]);

  const handleSet = () => {
    const urlToUse = webhookUrlInput.trim() || (statusState.kind === 'ok' ? statusState.url : '');
    setWebhook(true, urlToUse || undefined).then(() => fetchStatusAndInfo()).catch(() => {});
  };
  const handleUnset = () => setWebhook(false).then(() => fetchStatusAndInfo()).catch(() => {});
  const handleDelete = () => {
    setDeleteLoading(true);
    telegramApiRepository
      .deleteWebhook()
      .then(() => fetchStatusAndInfo())
      .catch(() => {})
      .finally(() => setDeleteLoading(false));
  };

  const loading = status === 'loading';

  return (
    <section>
      <h3>웹후크 설정 및 해제하기</h3>
      <hr />
      {statusState.kind === 'error' && (
        <p style={{ marginBottom: 8, fontSize: 14, color: '#856404', background: '#fff3cd', padding: 8, borderRadius: 4 }}>
          백엔드(8080)에 연결할 수 없습니다. 백엔드를 실행한 뒤 새로고침하세요.
        </p>
      )}
      {webhookInfo?.ok && webhookInfo.result?.url && (
        <div style={{ marginBottom: 12, padding: 10, background: '#f8f9fa', borderRadius: 6, fontSize: 13 }}>
          <strong>Telegram에 현재 등록된 URL</strong> (ngrok 재시작 시 자동으로 바뀌지 않음)
          <p style={{ margin: '6px 0 0', wordBreak: 'break-all' }}>
            <a href={webhookInfo.result.url} target="_blank" rel="noopener noreferrer">{webhookInfo.result.url}</a>
          </p>
          {typeof webhookInfo.result.pending_update_count === 'number' && webhookInfo.result.pending_update_count > 0 && (
            <p style={{ margin: '4px 0 0', color: '#856404' }}>
              대기 중인 업데이트: {webhookInfo.result.pending_update_count}건
              {webhookInfo.result.last_error_message && ` · 마지막 오류: ${webhookInfo.result.last_error_message}`}
            </p>
          )}
        </div>
      )}
      <p style={{ marginBottom: 6, fontSize: 14 }}>
        <label>
          웹후크에 등록할 URL (ngrok 실행 후 나온 <strong>현재</strong> 주소 + <code>/callback.do</code>)
        </label>
      </p>
      <input
        type="url"
        value={webhookUrlInput}
        onChange={(e) => setWebhookUrlInput(e.target.value)}
        placeholder="https://xxxx.ngrok-free.app/callback.do"
        style={{ width: '100%', maxWidth: 480, padding: 6, marginBottom: 8, boxSizing: 'border-box' }}
      />
      {statusState.kind === 'ok' && statusState.url && !webhookUrlInput && (
        <p style={{ marginBottom: 8, fontSize: 13, color: '#666' }}>
          설정(application.yml) 기준 URL: <a href={statusState.url} target="_blank" rel="noopener noreferrer" style={{ wordBreak: 'break-all' }}>{statusState.url}</a>
        </p>
      )}
      <p style={{ marginBottom: 8, fontSize: 12, color: '#666' }}>
        ngrok을 재실행하면 URL이 바뀝니다. <strong>지금 ngrok 터미널에 표시된 주소만</strong> 입력하세요. 예전에 복사해 둔 주소(예: 이전 세션의 bea7, e138 등)는 재시작 후에는 쓸 수 없어서 「호스트를 찾지 못했습니다」 오류가 납니다.
      </p>
      <div>
        <button type="button" onClick={handleSet} disabled={loading}>
          설정
        </button>
        <button type="button" onClick={handleUnset} disabled={loading}>
          해제
        </button>
        <button type="button" onClick={handleDelete} disabled={deleteLoading || loading}>
          {deleteLoading ? '처리 중…' : 'Telegram에서만 삭제'}
        </button>
      </div>
      <br />
      <h3>결과</h3>
      <hr />
      {error && <p style={{ color: 'red' }}>{error}</p>}
      {result != null && result.ok === true && result.description && (
        <>
          <p style={{ color: '#155724', background: '#d4edda', padding: 8, borderRadius: 4, marginBottom: 8 }}>
            {result.description === 'Webhook was set'
              ? '웹후크가 설정되었습니다.'
              : result.description === 'Webhook is already deleted'
                ? '이미 웹후크가 해제된 상태입니다.'
                : result.description}
          </p>
          {result.description === 'Webhook was set' && (
            <div style={{ marginBottom: 12, padding: 12, background: '#e7f3ff', borderRadius: 8, fontSize: 14 }}>
              <strong>다음 단계</strong>
              <ol style={{ marginTop: 8, marginBottom: 0, paddingLeft: 20 }}>
                <li>텔레그램에서 봇에게 아무 메시지나 보내기</li>
                <li>백엔드(Spring) 로그에서 <code>Webhook callback received: update_id=..., message.text=...</code> 확인</li>
                <li>ngrok 재실행 시 URL이 바뀌면 웹후크 다시 등록 필요</li>
              </ol>
            </div>
          )}
        </>
      )}
      {result != null && (
        <pre style={{ fontSize: 12 }}>{JSON.stringify(result, null, 2)}</pre>
      )}
    </section>
  );
}
