'use client';

import { useCallback, useRef, useState } from 'react';
import { useChannelBroadcast } from '@/application/hooks/useChannelBroadcast';

export function ChannelBroadcastPanel() {
  const [messageText, setMessageText] = useState('');
  const fileInputRef = useRef<HTMLInputElement>(null);
  const {
    sendMessage,
    uploadPhoto,
    messageStatus,
    photoStatus,
    lastUpdated,
    error,
  } = useChannelBroadcast();

  const handleSendMessage = useCallback(async () => {
    const trimmed = messageText.trim();
    if (!trimmed) {
      alert('전송할 메시지를 입력하세요.');
      return;
    }
    setMessageText('');
    await sendMessage(trimmed).catch(() => {});
  }, [messageText, sendMessage]);

  const handleFileSubmit = useCallback(
    async (e: React.FormEvent<HTMLFormElement>) => {
      e.preventDefault();
      const file = fileInputRef.current?.files?.[0];
      if (!file) {
        alert('전송할 이미지 파일을 지정하세요.');
        return;
      }
      await uploadPhoto(file).catch(() => {});
      if (fileInputRef.current) fileInputRef.current.value = '';
    },
    [uploadPhoto]
  );

  const messageStatusText =
    messageStatus === 'loading'
      ? '전송 중입니다.'
      : messageStatus === 'success'
        ? '메시지를 전송했습니다.'
        : messageStatus === 'error'
          ? '전송에 실패했습니다.'
          : null;

  const photoStatusText =
    photoStatus === 'loading'
      ? '전송 중입니다.'
      : photoStatus === 'success'
        ? '이미지 파일을 전송했습니다.'
        : photoStatus === 'error'
          ? '전송에 실패했습니다.'
          : null;

  return (
    <section>
      <h3>채널에 메시지 전송</h3>
      <input
        type="text"
        value={messageText}
        onChange={(e) => setMessageText(e.target.value)}
        placeholder="전송할 메시지"
        size={100}
      />
      <button
        type="button"
        onClick={handleSendMessage}
        disabled={messageStatus === 'loading'}
      >
        전송
      </button>
      <hr />
      <h3>채널에 이미지 전송</h3>
      <form onSubmit={handleFileSubmit}>
        <input type="file" ref={fileInputRef} accept="image/*" />
        <button type="submit" disabled={photoStatus === 'loading'}>
          확인
        </button>
      </form>
      <hr />
      <h3>전송 결과</h3>
      <p>마지막 전송 시간: {lastUpdated?.toLocaleString() ?? '-'}</p>
      {error && <p style={{ color: 'red' }}>{error}</p>}
      {(messageStatusText || photoStatusText) && (
        <p>{messageStatusText ?? photoStatusText}</p>
      )}
    </section>
  );
}
