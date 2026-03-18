'use client';

import { useEffect, useRef, useState } from 'react';
import { useAiChat } from '@/application/hooks/useAiChat';

type AiChatPanelProps = {
  enabled: boolean;
};

const statusText: Record<'idle' | 'connecting' | 'streaming' | 'error', string> = {
  idle: '대기 중',
  connecting: 'AiServer 연결 중',
  streaming: '응답 스트리밍 중',
  error: '연결 오류',
};

export function AiChatPanel({ enabled }: AiChatPanelProps) {
  const { messages, isLoading, sendMessage, resetConversation, status } = useAiChat(enabled);
  const [draft, setDraft] = useState('');
  const messagesEndRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth', block: 'end' });
  }, [messages, isLoading]);

  const handleSend = () => {
    const trimmed = draft.trim();
    if (!trimmed || !enabled || isLoading) {
      return;
    }
    sendMessage(trimmed);
    setDraft('');
  };

  return (
    <section className="card stack ai-chat-panel">
      <div className="row" style={{ justifyContent: 'space-between' }}>
        <div className="stack" style={{ gap: 4 }}>
          <strong>AiServer 대화</strong>
          <span className="muted" style={{ fontSize: 13 }}>
            시작 버튼을 누른 뒤 질문을 보내면 `http://localhost:6060`의 AiServer와 대화합니다.
          </span>
        </div>
        <div className="row">
          <span className={`notice ${status === 'error' ? 'notice-warning' : 'notice-info'}`}>
            {statusText[status]}
          </span>
          <button type="button" onClick={resetConversation} disabled={isLoading}>
            초기화
          </button>
        </div>
      </div>

      {!enabled && (
        <p className="notice notice-warning">
          위의 시작 버튼을 한 번 눌러야 AI 대화 입력창이 활성화됩니다.
        </p>
      )}

      <div className="ai-chat-messages">
        {messages.map((message) => (
          <div
            key={message.id}
            className={`ai-chat-message ${message.role === 'user' ? 'ai-chat-message-user' : 'ai-chat-message-assistant'}`}
          >
            <div className="muted" style={{ fontSize: 12, marginBottom: 4 }}>
              {message.role === 'user' ? '나' : 'AI'}
            </div>
            <div style={{ whiteSpace: 'pre-wrap' }}>{message.content}</div>
          </div>
        ))}
        {isLoading && (
          <div className="ai-chat-message ai-chat-message-assistant">
            <div className="muted" style={{ fontSize: 12, marginBottom: 4 }}>
              AI
            </div>
            <div className="ai-chat-typing">응답을 작성하고 있습니다...</div>
          </div>
        )}
        <div ref={messagesEndRef} />
      </div>

      <div className="row ai-chat-input-row">
        <textarea
          value={draft}
          onChange={(event) => setDraft(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === 'Enter' && !event.shiftKey) {
              event.preventDefault();
              handleSend();
            }
          }}
          rows={3}
          disabled={!enabled || isLoading}
          placeholder={
            enabled
              ? 'AiServer에 물어볼 내용을 입력하세요. Enter로 전송, Shift+Enter로 줄바꿈.'
              : '먼저 위의 시작 버튼을 눌러 주세요.'
          }
          className="ai-chat-input"
        />
        <button type="button" onClick={handleSend} disabled={!enabled || isLoading || draft.trim() === ''}>
          전송
        </button>
      </div>
    </section>
  );
}
