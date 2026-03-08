'use client';

import { useCallback, useRef, useState } from 'react';
import {
  useChannelBroadcast,
  type SendType,
} from '@/application/hooks/useChannelBroadcast';

const SEND_TYPE_OPTIONS: { value: SendType; label: string }[] = [
  { value: 'message', label: '메시지' },
  { value: 'photo', label: '이미지' },
  { value: 'document', label: '파일(문서)' },
];

export function ChannelBroadcastPanel() {
  const [sendType, setSendType] = useState<SendType>('message');
  const [messageText, setMessageText] = useState('');
  const [photoCaption, setPhotoCaption] = useState('');
  const [documentCaption, setDocumentCaption] = useState('');
  const photoInputRef = useRef<HTMLInputElement>(null);
  const documentInputRef = useRef<HTMLInputElement>(null);
  const {
    sendMessage,
    uploadPhoto,
    uploadDocument,
    messageStatus,
    photoStatus,
    documentStatus,
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

  const handlePhotoSubmit = useCallback(
    async (e: React.FormEvent<HTMLFormElement>) => {
      e.preventDefault();
      const file = photoInputRef.current?.files?.[0];
      if (!file) {
        alert('전송할 이미지 파일을 지정하세요.');
        return;
      }
      const cap = photoCaption.trim().slice(0, 1024) || undefined;
      await uploadPhoto(file, cap).catch(() => {});
      if (photoInputRef.current) photoInputRef.current.value = '';
      setPhotoCaption('');
    },
    [uploadPhoto, photoCaption]
  );

  const handleDocumentSubmit = useCallback(
    async (e: React.FormEvent<HTMLFormElement>) => {
      e.preventDefault();
      const file = documentInputRef.current?.files?.[0];
      if (!file) {
        alert('전송할 파일을 지정하세요.');
        return;
      }
      const cap = documentCaption.trim().slice(0, 1024) || undefined;
      await uploadDocument(file, cap).catch(() => {});
      if (documentInputRef.current) documentInputRef.current.value = '';
      setDocumentCaption('');
    },
    [uploadDocument, documentCaption]
  );

  const statusByType: Record<SendType, string | null> = {
    message:
      messageStatus === 'loading'
        ? '전송 중입니다.'
        : messageStatus === 'success'
          ? '메시지를 전송했습니다.'
          : messageStatus === 'error'
            ? '전송에 실패했습니다.'
            : null,
    photo:
      photoStatus === 'loading'
        ? '전송 중입니다.'
        : photoStatus === 'success'
          ? '이미지를 전송했습니다.'
          : photoStatus === 'error'
            ? '전송에 실패했습니다.'
            : null,
    document:
      documentStatus === 'loading'
        ? '전송 중입니다.'
        : documentStatus === 'success'
          ? '파일을 전송했습니다.'
          : documentStatus === 'error'
            ? '전송에 실패했습니다.'
            : null,
  };
  const lastStatusText =
    statusByType.message ?? statusByType.photo ?? statusByType.document;

  return (
    <section>
      <h3>채널에 보내기</h3>
      <p>
        <label>
          보내기 유형:{' '}
          <select
            value={sendType}
            onChange={(e) => setSendType(e.target.value as SendType)}
          >
            {SEND_TYPE_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        </label>
      </p>

      {sendType === 'message' && (
        <p>
          <input
            type="text"
            value={messageText}
            onChange={(e) => setMessageText(e.target.value)}
            placeholder="전송할 메시지"
            size={80}
          />
          <button
            type="button"
            onClick={handleSendMessage}
            disabled={messageStatus === 'loading'}
          >
            전송
          </button>
        </p>
      )}

      {sendType === 'photo' && (
        <form onSubmit={handlePhotoSubmit}>
          <p>
            <input
              type="file"
              ref={photoInputRef}
              accept="image/*"
              aria-label="이미지 파일 선택"
            />
          </p>
          <p>
            <input
              type="text"
              value={photoCaption}
              onChange={(e) => setPhotoCaption(e.target.value)}
              placeholder="이미지와 함께 보낼 메시지 (선택, 최대 1024자)"
              size={60}
              maxLength={1024}
              aria-label="캡션"
            />
          </p>
          <button type="submit" disabled={photoStatus === 'loading'}>
            전송
          </button>
        </form>
      )}

      {sendType === 'document' && (
        <form onSubmit={handleDocumentSubmit}>
          <p>
            <input
              type="file"
              ref={documentInputRef}
              aria-label="파일 선택"
            />
          </p>
          <p>
            <input
              type="text"
              value={documentCaption}
              onChange={(e) => setDocumentCaption(e.target.value)}
              placeholder="파일과 함께 보낼 메시지 (선택, 최대 1024자)"
              size={60}
              maxLength={1024}
              aria-label="캡션"
            />
          </p>
          <button type="submit" disabled={documentStatus === 'loading'}>
            전송
          </button>
        </form>
      )}

      <hr />
      <h3>전송 결과</h3>
      <p>마지막 전송 시간: {lastUpdated?.toLocaleString() ?? '-'}</p>
      {error && <p style={{ color: 'red' }}>{error}</p>}
      {lastStatusText && <p>{lastStatusText}</p>}
    </section>
  );
}
