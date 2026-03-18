'use client';

import Image from 'next/image';
import { useCallback, useEffect, useState, type CSSProperties, type FormEvent } from 'react';
import { telegramApiRepository } from '@/infrastructure/api/TelegramApiRepository';
import type { FileHistoryItem, MessageHistoryItem } from '@/domain/types/api';

const PAGE_SIZE = 20;

type Tab = 'messages' | 'files';

const tabButtonStyle = (active: boolean): CSSProperties => ({
  padding: '10px 18px',
  fontWeight: active ? 700 : 500,
  border: '1px solid #1f2937',
  background: active ? '#e5eefb' : '#ffffff',
  color: '#111827',
  cursor: 'pointer',
  borderRadius: 999,
});

const fileGridStyle: CSSProperties = {
  display: 'grid',
  gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))',
  gap: 16,
};

const fileCardStyle: CSSProperties = {
  border: '1px solid #d1d5db',
  borderRadius: 18,
  overflow: 'hidden',
  background: '#ffffff',
  boxShadow: '0 10px 30px rgba(15, 23, 42, 0.08)',
};

const previewFrameStyle: CSSProperties = {
  position: 'relative',
  height: 220,
  background: 'linear-gradient(135deg, #e5eefb 0%, #f8fafc 100%)',
};

const fileBodyStyle: CSSProperties = {
  display: 'grid',
  gap: 8,
  padding: 16,
};

const fileNameStyle: CSSProperties = {
  fontSize: 16,
  fontWeight: 700,
  color: '#111827',
  wordBreak: 'break-word',
};

const fileMetaStyle: CSSProperties = {
  fontSize: 13,
  color: '#475569',
  wordBreak: 'break-word',
};

const objectKeyStyle: CSSProperties = {
  ...fileMetaStyle,
  fontFamily: 'monospace',
  fontSize: 12,
  color: '#64748b',
};

const actionLinkStyle: CSSProperties = {
  display: 'inline-flex',
  alignItems: 'center',
  justifyContent: 'center',
  width: 'fit-content',
  padding: '8px 12px',
  borderRadius: 999,
  border: '1px solid #2563eb',
  color: '#2563eb',
  textDecoration: 'none',
  fontWeight: 600,
};

const emptyCardStyle: CSSProperties = {
  padding: 24,
  border: '1px dashed #cbd5e1',
  borderRadius: 18,
  color: '#64748b',
  background: '#f8fafc',
};

const fileTypeBadgeStyle: CSSProperties = {
  position: 'absolute',
  inset: 0,
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  fontSize: 36,
  fontWeight: 800,
  color: '#1d4ed8',
  letterSpacing: 1,
};

const isBrokenText = (value: string | null | undefined): boolean => {
  const normalized = value?.trim();
  return !normalized || /^\?+$/.test(normalized) || normalized.includes('�');
};

const resolveSenderLabel = (
  rawName: string | null | undefined,
  fallback: string
): string => (isBrokenText(rawName) ? fallback : rawName!.trim());

const isImageFile = (file: FileHistoryItem): boolean => {
  const contentType = file.contentType ?? '';
  if (contentType.startsWith('image/')) return true;
  return /\.(png|jpe?g|gif|webp|bmp|svg)$/i.test(file.fileName);
};

const getFileTypeLabel = (fileName: string): string => {
  const extension = fileName.split('.').pop()?.trim().toUpperCase();
  return extension && extension.length <= 5 ? extension : 'FILE';
};

const formatDate = (value: string): string => {
  try {
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? value : date.toLocaleString('ko-KR');
  } catch {
    return value;
  }
};

export default function ChannelHistoryPage() {
  const [tab, setTab] = useState<Tab>('messages');
  const [messagePage, setMessagePage] = useState(0);
  const [filePage, setFilePage] = useState(0);
  const [messageSearch, setMessageSearch] = useState('');
  const [fileSearch, setFileSearch] = useState('');
  const [messageSearchInput, setMessageSearchInput] = useState('');
  const [fileSearchInput, setFileSearchInput] = useState('');
  const [messages, setMessages] = useState<MessageHistoryItem[]>([]);
  const [files, setFiles] = useState<FileHistoryItem[]>([]);
  const [messageTotal, setMessageTotal] = useState(0);
  const [fileTotal, setFileTotal] = useState(0);
  const [messageTotalPages, setMessageTotalPages] = useState(0);
  const [fileTotalPages, setFileTotalPages] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadMessages = useCallback(async (page: number, search?: string) => {
    setLoading(true);
    setError(null);

    try {
      const response = await telegramApiRepository.getMessageHistory(page, PAGE_SIZE, search);
      if (response.data) {
        setMessages(response.data.content);
        setMessageTotal(response.data.totalElements);
        setMessageTotalPages(response.data.totalPages);
      } else {
        setMessages([]);
        setMessageTotal(0);
        setMessageTotalPages(0);
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : '메시지 이력을 불러오지 못했습니다.');
      setMessages([]);
    } finally {
      setLoading(false);
    }
  }, []);

  const loadFiles = useCallback(async (page: number, search?: string) => {
    setLoading(true);
    setError(null);

    try {
      const response = await telegramApiRepository.getFileHistory(page, PAGE_SIZE, search);
      if (response.data) {
        setFiles(response.data.content);
        setFileTotal(response.data.totalElements);
        setFileTotalPages(response.data.totalPages);
      } else {
        setFiles([]);
        setFileTotal(0);
        setFileTotalPages(0);
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : '파일 이력을 불러오지 못했습니다.');
      setFiles([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (tab === 'messages') {
      loadMessages(messagePage, messageSearch || undefined);
    }
  }, [tab, messagePage, messageSearch, loadMessages]);

  useEffect(() => {
    if (tab === 'files') {
      loadFiles(filePage, fileSearch || undefined);
    }
  }, [tab, filePage, fileSearch, loadFiles]);

  useEffect(() => {
    const eventSource = new EventSource(telegramApiRepository.getMessageHistoryEventsUrl());

    const refresh = (kind?: string) => {
      if (kind === 'file') {
        if (tab === 'files') {
          loadFiles(filePage, fileSearch || undefined);
        }
        return;
      }

      if (tab === 'messages') {
        loadMessages(messagePage, messageSearch || undefined);
      }
    };

    const onRefresh = (event: MessageEvent) => {
      refresh(typeof event.data === 'string' ? event.data : undefined);
    };

    eventSource.addEventListener('history-refresh', onRefresh as EventListener);
    eventSource.onerror = () => {
      setError((previous) => previous ?? '실시간 연결이 잠시 끊겼습니다. 자동으로 다시 연결합니다.');
    };

    return () => {
      eventSource.removeEventListener('history-refresh', onRefresh as EventListener);
      eventSource.close();
    };
  }, [tab, messagePage, filePage, messageSearch, fileSearch, loadMessages, loadFiles]);

  const handleMessageSearch = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setMessageSearch(messageSearchInput.trim());
    setMessagePage(0);
  };

  const handleFileSearch = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setFileSearch(fileSearchInput.trim());
    setFilePage(0);
  };

  return (
    <div className="chat-layout">
      <div className="chat-container">
        <header className="chat-header">
          텔레그램 수신 이력
          <span className="chat-header-sub">메시지·파일 업로드 이력을 페이징/검색합니다.</span>
        </header>

        <div className="chat-messages">
          <div style={{ marginBottom: 16, display: 'flex', gap: 8, flexWrap: 'wrap' }}>
            <button type="button" onClick={() => setTab('messages')} style={tabButtonStyle(tab === 'messages')}>
              메시지 이력
            </button>
            <button type="button" onClick={() => setTab('files')} style={tabButtonStyle(tab === 'files')}>
              파일 갤러리
            </button>
          </div>

          {error && <p style={{ color: '#dc2626', marginBottom: 12 }}>{error}</p>}
          {loading && <p>불러오는 중...</p>}

          {tab === 'messages' && (
            <section className="section">
              <form onSubmit={handleMessageSearch} style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                <input
                  type="text"
                  value={messageSearchInput}
                  onChange={(event) => setMessageSearchInput(event.target.value)}
                  placeholder="메시지 내용 검색"
                />
                <button type="submit">검색</button>
              </form>

              <table className="table">
                <thead>
                  <tr>
                    <th>수신 시각</th>
                    <th>보낸 사람</th>
                    <th>채팅 ID</th>
                    <th>내용</th>
                  </tr>
                </thead>
                <tbody>
                  {messages.length === 0 && !loading && (
                    <tr>
                      <td colSpan={4} style={{ padding: 16, color: '#64748b' }}>
                        메시지 이력이 없습니다.
                      </td>
                    </tr>
                  )}

                  {messages.map((message) => (
                    <tr key={message.id}>
                      <td>{formatDate(message.receivedAt)}</td>
                      <td>{resolveSenderLabel(message.fromUserName, message.fromUserId)}</td>
                      <td>{message.chatId}</td>
                      <td style={{ maxWidth: 420, overflow: 'hidden', textOverflow: 'ellipsis' }}>
                        {(message.text ?? '').slice(0, 200)}
                        {(message.text?.length ?? 0) > 200 ? '...' : ''}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>

              <div style={{ marginTop: 8, display: 'flex', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>
                <button type="button" disabled={messagePage <= 0 || loading} onClick={() => setMessagePage((page) => page - 1)}>
                  이전
                </button>
                <span>
                  {messagePage + 1} / {Math.max(1, messageTotalPages)} (총 {messageTotal}건)
                </span>
                <button
                  type="button"
                  disabled={messagePage >= messageTotalPages - 1 || loading}
                  onClick={() => setMessagePage((page) => page + 1)}
                >
                  다음
                </button>
              </div>
            </section>
          )}

          {tab === 'files' && (
            <section className="section">
              <form onSubmit={handleFileSearch} style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                <input
                  type="text"
                  value={fileSearchInput}
                  onChange={(event) => setFileSearchInput(event.target.value)}
                  placeholder="파일명 검색"
                />
                <button type="submit">검색</button>
              </form>

              <div className="muted">
                MinIO 원본 기준으로 이미지 미리보기를 제공하고, 문서 파일은 바로 열어볼 수 있습니다.
              </div>

              <div style={fileGridStyle}>
                {files.length === 0 && !loading && <div style={emptyCardStyle}>파일 이력이 없습니다.</div>}

                {files.map((file) => {
                  const previewUrl = telegramApiRepository.getFilePreviewUrl(file.objectKey);
                  const senderLabel = resolveSenderLabel(file.fromUserName, '알 수 없음');
                  const imageFile = isImageFile(file);

                  return (
                    <article key={file.id} style={fileCardStyle}>
                      <div style={previewFrameStyle}>
                        {imageFile ? (
                          <Image
                            src={previewUrl}
                            alt={file.fileName}
                            fill
                            unoptimized
                            sizes="(max-width: 768px) 100vw, 33vw"
                            style={{ objectFit: 'cover' }}
                          />
                        ) : (
                          <div style={fileTypeBadgeStyle}>{getFileTypeLabel(file.fileName)}</div>
                        )}
                      </div>

                      <div style={fileBodyStyle}>
                        <div style={fileNameStyle}>{file.fileName}</div>
                        <div style={fileMetaStyle}>보낸 사람: {senderLabel}</div>
                        <div style={fileMetaStyle}>저장 시각: {formatDate(file.createdAt)}</div>
                        <div style={fileMetaStyle}>형식: {file.contentType ?? 'application/octet-stream'}</div>
                        <div style={objectKeyStyle}>{file.objectKey}</div>
                        <a href={previewUrl} target="_blank" rel="noreferrer" style={actionLinkStyle}>
                          {imageFile ? '원본 보기' : '파일 열기'}
                        </a>
                      </div>
                    </article>
                  );
                })}
              </div>

              <div style={{ marginTop: 8, display: 'flex', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>
                <button type="button" disabled={filePage <= 0 || loading} onClick={() => setFilePage((page) => page - 1)}>
                  이전
                </button>
                <span>
                  {filePage + 1} / {Math.max(1, fileTotalPages)} (총 {fileTotal}건)
                </span>
                <button
                  type="button"
                  disabled={filePage >= fileTotalPages - 1 || loading}
                  onClick={() => setFilePage((page) => page + 1)}
                >
                  다음
                </button>
              </div>
            </section>
          )}
        </div>
      </div>
    </div>
  );
}
