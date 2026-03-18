'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import { createChatMessage, type ChatMessage } from '@/domain/chat';
import { aiServerApiRepository } from '@/infrastructure/api/AiServerApiRepository';

function generateUserId(): string {
  return `frontend-user-${Date.now()}-${Math.random().toString(36).slice(2, 11)}`;
}

const INITIAL_MESSAGE = '시작 버튼을 누른 뒤 메시지를 보내면 AiServer와 바로 대화할 수 있어요.';
const CONNECTION_ERROR_MESSAGE = 'AiServer SSE 연결에 실패했습니다. 6060 포트가 실행 중인지 확인해 주세요.';
const SEND_ERROR_MESSAGE = 'AiServer로 메시지를 보내지 못했습니다. 서버 상태를 확인해 주세요.';

export function useAiChat(enabled: boolean) {
  const [messages, setMessages] = useState<ChatMessage[]>(() => [
    createChatMessage('assistant', INITIAL_MESSAGE),
  ]);
  const [isLoading, setIsLoading] = useState(false);
  const [status, setStatus] = useState<'idle' | 'connecting' | 'streaming' | 'error'>('idle');

  const userIdRef = useRef<string>(generateUserId());
  const eventSourceRef = useRef<EventSource | null>(null);
  const currentAssistantMessageIdRef = useRef<string | null>(null);
  const closingRef = useRef(false);

  const closeSse = useCallback(() => {
    closingRef.current = true;
    if (eventSourceRef.current) {
      eventSourceRef.current.close();
      eventSourceRef.current = null;
    }
  }, []);

  useEffect(() => () => closeSse(), [closeSse]);

  const appendAssistantChunk = useCallback((chunk: string) => {
    if (aiServerApiRepository.isIgnorableChunk(chunk)) {
      return;
    }

    setMessages((prev) => {
      const currentId = currentAssistantMessageIdRef.current;
      const lastMessage = prev[prev.length - 1];

      if (
        currentId != null &&
        lastMessage?.role === 'assistant' &&
        lastMessage.id === currentId
      ) {
        return [
          ...prev.slice(0, -1),
          { ...lastMessage, content: `${lastMessage.content}${chunk}` },
        ];
      }

      const newMessage = createChatMessage('assistant', chunk);
      currentAssistantMessageIdRef.current = newMessage.id;
      return [...prev, newMessage];
    });
  }, []);

  const finishStream = useCallback((finalChunk?: string) => {
    if (finalChunk != null && !aiServerApiRepository.isIgnorableChunk(finalChunk)) {
      appendAssistantChunk(finalChunk);
    }

    currentAssistantMessageIdRef.current = null;
    setIsLoading(false);
    setStatus('idle');
    closeSse();
  }, [appendAssistantChunk, closeSse]);

  const sendMessage = useCallback(async (content: string) => {
    const text = content.trim();
    if (!enabled || !text || isLoading) {
      return;
    }

    setMessages((prev) => [...prev, createChatMessage('user', text)]);
    setIsLoading(true);
    setStatus('connecting');
    currentAssistantMessageIdRef.current = null;

    closeSse();
    closingRef.current = false;

    const eventSource = new EventSource(aiServerApiRepository.buildSseUrl(userIdRef.current));
    eventSourceRef.current = eventSource;

    eventSource.addEventListener('add', (event) => {
      if (closingRef.current) {
        return;
      }
      setStatus('streaming');
      appendAssistantChunk(event.data ?? '');
    });

    eventSource.addEventListener('finish', (event) => {
      if (closingRef.current) {
        return;
      }
      finishStream(event.data ?? '');
    });

    eventSource.onerror = () => {
      if (closingRef.current) {
        return;
      }

      currentAssistantMessageIdRef.current = null;
      setIsLoading(false);
      setStatus('error');
      closeSse();
      setMessages((prev) => [...prev, createChatMessage('assistant', CONNECTION_ERROR_MESSAGE)]);
    };

    try {
      const response = await aiServerApiRepository.sendChatAi(userIdRef.current, text);
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }
    } catch {
      currentAssistantMessageIdRef.current = null;
      setIsLoading(false);
      setStatus('error');
      closeSse();
      setMessages((prev) => [...prev, createChatMessage('assistant', SEND_ERROR_MESSAGE)]);
    }
  }, [appendAssistantChunk, closeSse, enabled, finishStream, isLoading]);

  const resetConversation = useCallback(() => {
    closeSse();
    currentAssistantMessageIdRef.current = null;
    setIsLoading(false);
    setStatus('idle');
    setMessages([createChatMessage('assistant', INITIAL_MESSAGE)]);
  }, [closeSse]);

  return {
    enabled,
    isLoading,
    messages,
    resetConversation,
    sendMessage,
    status,
    userId: userIdRef.current,
  };
}
