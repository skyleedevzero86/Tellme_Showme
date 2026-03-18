'use client';

import { useState, useCallback, useEffect } from 'react';
import Link from 'next/link';
import { telegramApiRepository } from '@/infrastructure/api/TelegramApiRepository';
import type { MessageHistoryItem, FileHistoryItem } from '@/domain/types/api';

const PAGE_SIZE = 20;

type Tab = 'messages' | 'files';

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
      const res = await telegramApiRepository.getMessageHistory(page, PAGE_SIZE, search);
      if (res.data) {
        setMessages(res.data.content);
        setMessageTotal(res.data.totalElements);
        setMessageTotalPages(res.data.totalPages);
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
      const res = await telegramApiRepository.getFileHistory(page, PAGE_SIZE, search);
      if (res.data) {
        setFiles(res.data.content);
        setFileTotal(res.data.totalElements);
        setFileTotalPages(res.data.totalPages);
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
    if (tab === 'messages') loadMessages(messagePage, messageSearch || undefined);
  }, [tab, messagePage, messageSearch, loadMessages]);

  useEffect(() => {
    if (tab === 'files') loadFiles(filePage, fileSearch || undefined);
  }, [tab, filePage, fileSearch, loadFiles]);

  const handleMessageSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setMessageSearch(messageSearchInput.trim());
    setMessagePage(0);
  };

  const handleFileSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setFileSearch(fileSearchInput.trim());
    setFilePage(0);
  };

  const formatDate = (s: string) => {
    try {
      const d = new Date(s);
      return Number.isNaN(d.getTime()) ? s : d.toLocaleString('ko-KR');
    } catch {
      return s;
    }
  };

  return (
    <>
      <p style={{ marginBottom: 16 }}>
        <Link href="/channel">← 채널 브로드캐스트</Link>
      </p>
      <h2>받은 메시지 이력</h2>
      <p>텔레그램에서 수신한 메시지와 파일(MinIO 암호화 저장) 이력을 검색·페이징하여 볼 수 있습니다.</p>
      <hr />

      <div style={{ marginBottom: 16, display: 'flex', gap: 8 }}>
        <button
          type="button"
          onClick={() => setTab('messages')}
          style={{
            padding: '8px 16px',
            fontWeight: tab === 'messages' ? 'bold' : 'normal',
            border: '1px solid #333',
            background: tab === 'messages' ? '#e0e0e0' : 'transparent',
            cursor: 'pointer',
          }}
        >
          메시지 이력
        </button>
        <button
          type="button"
          onClick={() => setTab('files')}
          style={{
            padding: '8px 16px',
            fontWeight: tab === 'files' ? 'bold' : 'normal',
            border: '1px solid #333',
            background: tab === 'files' ? '#e0e0e0' : 'transparent',
            cursor: 'pointer',
          }}
        >
          파일 이력
        </button>
      </div>

      {error && <p style={{ color: 'red', marginBottom: 8 }}>{error}</p>}
      {loading && <p>불러오는 중…</p>}

      {tab === 'messages' && (
        <section>
          <form onSubmit={handleMessageSearch} style={{ marginBottom: 16, display: 'flex', gap: 8 }}>
            <input
              type="text"
              value={messageSearchInput}
              onChange={(e) => setMessageSearchInput(e.target.value)}
              placeholder="메시지 내용 검색"
              style={{ padding: '6px 10px', width: 260 }}
            />
            <button type="submit">검색</button>
          </form>
          <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 14 }}>
            <thead>
              <tr style={{ borderBottom: '2px solid #333', textAlign: 'left' }}>
                <th style={{ padding: 8 }}>수신 시각</th>
                <th style={{ padding: 8 }}>보낸 사람</th>
                <th style={{ padding: 8 }}>채팅 ID</th>
                <th style={{ padding: 8 }}>내용</th>
              </tr>
            </thead>
            <tbody>
              {messages.length === 0 && !loading && (
                <tr>
                  <td colSpan={4} style={{ padding: 16, color: '#666' }}>
                    메시지가 없습니다.
                  </td>
                </tr>
              )}
              {messages.map((m) => (
                <tr key={m.id} style={{ borderBottom: '1px solid #ddd' }}>
                  <td style={{ padding: 8 }}>{formatDate(m.receivedAt)}</td>
                  <td style={{ padding: 8 }}>{m.fromUserName ?? m.fromUserId}</td>
                  <td style={{ padding: 8 }}>{m.chatId}</td>
                  <td style={{ padding: 8, maxWidth: 400, overflow: 'hidden', textOverflow: 'ellipsis' }}>
                    {(m.text ?? '').slice(0, 200)}
                    {(m.text?.length ?? 0) > 200 ? '…' : ''}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <div style={{ marginTop: 16, display: 'flex', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>
            <button
              type="button"
              disabled={messagePage <= 0 || loading}
              onClick={() => setMessagePage((p) => p - 1)}
            >
              이전
            </button>
            <span>
              {messagePage + 1} / {Math.max(1, messageTotalPages)} (총 {messageTotal}건)
            </span>
            <button
              type="button"
              disabled={messagePage >= messageTotalPages - 1 || loading}
              onClick={() => setMessagePage((p) => p + 1)}
            >
              다음
            </button>
          </div>
        </section>
      )}

      {tab === 'files' && (
        <section>
          <form onSubmit={handleFileSearch} style={{ marginBottom: 16, display: 'flex', gap: 8 }}>
            <input
              type="text"
              value={fileSearchInput}
              onChange={(e) => setFileSearchInput(e.target.value)}
              placeholder="파일명 검색"
              style={{ padding: '6px 10px', width: 260 }}
            />
            <button type="submit">검색</button>
          </form>
          <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 14 }}>
            <thead>
              <tr style={{ borderBottom: '2px solid #333', textAlign: 'left' }}>
                <th style={{ padding: 8 }}>저장 시각</th>
                <th style={{ padding: 8 }}>보낸 사람</th>
                <th style={{ padding: 8 }}>파일명</th>
                <th style={{ padding: 8 }}>Content-Type</th>
                <th style={{ padding: 8 }}>객체 키</th>
              </tr>
            </thead>
            <tbody>
              {files.length === 0 && !loading && (
                <tr>
                  <td colSpan={5} style={{ padding: 16, color: '#666' }}>
                    파일 이력이 없습니다.
                  </td>
                </tr>
              )}
              {files.map((f) => (
                <tr key={f.id} style={{ borderBottom: '1px solid #ddd' }}>
                  <td style={{ padding: 8 }}>{formatDate(f.createdAt)}</td>
                  <td style={{ padding: 8 }}>{f.fromUserName ?? f.userId}</td>
                  <td style={{ padding: 8 }}>{f.fileName}</td>
                  <td style={{ padding: 8 }}>{f.contentType}</td>
                  <td style={{ padding: 8, maxWidth: 200, overflow: 'hidden', textOverflow: 'ellipsis' }}>
                    {f.objectKey}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <div style={{ marginTop: 16, display: 'flex', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>
            <button
              type="button"
              disabled={filePage <= 0 || loading}
              onClick={() => setFilePage((p) => p - 1)}
            >
              이전
            </button>
            <span>
              {filePage + 1} / {Math.max(1, fileTotalPages)} (총 {fileTotal}건)
            </span>
            <button
              type="button"
              disabled={filePage >= fileTotalPages - 1 || loading}
              onClick={() => setFilePage((p) => p + 1)}
            >
              다음
            </button>
          </div>
        </section>
      )}
    </>
  );
}
