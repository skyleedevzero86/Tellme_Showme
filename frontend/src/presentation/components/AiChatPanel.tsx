'use client';

import { useEffect, useRef, useState } from 'react';
import { useAiChat } from '@/application/hooks/useAiChat';

type AiChatPanelProps = {
  enabled: boolean;
};

const statusText: Record<'idle' | 'connecting' | 'streaming' | 'error', string> = {
  idle: 'Ready',
  connecting: 'Creating reply',
  streaming: 'Receiving reply',
  error: 'Connection error',
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
          <strong>Unified Chat Input</strong>
          <span className="muted" style={{ fontSize: 13 }}>
            This panel uses the same parser as Telegram. You can test normal chat and commands like
            {' '}
            <code>/god</code>, <code>/eng</code>, and <code>/search</code> here.
          </span>
        </div>
        <div className="row">
          <span className={`notice ${status === 'error' ? 'notice-warning' : 'notice-info'}`}>
            {statusText[status]}
          </span>
          <button type="button" onClick={resetConversation} disabled={isLoading}>
            Reset
          </button>
        </div>
      </div>

      {!enabled && (
        <p className="notice notice-warning">
          Start the chat first. Once enabled, the frontend will follow the same input flow as Telegram.
        </p>
      )}

      <div className="ai-chat-messages">
        {messages.map((message) => (
          <div
            key={message.id}
            className={`ai-chat-message ${message.role === 'user' ? 'ai-chat-message-user' : 'ai-chat-message-assistant'}`}
          >
            <div className="muted" style={{ fontSize: 12, marginBottom: 4 }}>
              {message.role === 'user' ? 'You' : 'AI'}
            </div>
            <div style={{ whiteSpace: 'pre-wrap' }}>{message.content}</div>
          </div>
        ))}
        {isLoading && (
          <div className="ai-chat-message ai-chat-message-assistant">
            <div className="muted" style={{ fontSize: 12, marginBottom: 4 }}>
              AI
            </div>
            <div className="ai-chat-typing">Creating a reply...</div>
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
              ? 'Try normal chat or commands like /god, /eng, and /search. Press Enter to send and Shift+Enter for a new line.'
              : 'Start the chat to enable input.'
          }
          className="ai-chat-input"
        />
        <button type="button" onClick={handleSend} disabled={!enabled || isLoading || draft.trim() === ''}>
          Send
        </button>
      </div>
    </section>
  );
}
