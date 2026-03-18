'use client';

import { useState } from 'react';
import { AiChatPanel } from '@/presentation/components/AiChatPanel';
import { GetUpdatesPanel } from '@/presentation/components/GetUpdatesPanel';

export default function GetUpdatesPage() {
  const [aiChatEnabled, setAiChatEnabled] = useState(false);

  return (
    <div className="chat-layout">
      <div className="chat-container">
        <header className="chat-header">
          Long Polling (getUpdates)
          <span className="chat-header-sub">주기적으로 새 메시지를 조회하고, 아래에서 AiServer와 대화합니다.</span>
        </header>
        <div className="chat-messages">
          <div className="card">
            <strong>안내</strong>
            <ul className="muted" style={{ marginTop: 8, paddingLeft: 18 }}>
              <li>시작 버튼으로 기존 getUpdates 폴링을 실행할 수 있습니다.</li>
              <li>시작 버튼을 한 번 누르면 같은 화면 아래에서 AiServer 채팅 입력창도 활성화됩니다.</li>
              <li>AiServer는 기본값으로 `http://localhost:6060`에 연결됩니다.</li>
            </ul>
          </div>
          <GetUpdatesPanel onStarted={() => setAiChatEnabled(true)} />
          <AiChatPanel enabled={aiChatEnabled} />
        </div>
      </div>
    </div>
  );
}
